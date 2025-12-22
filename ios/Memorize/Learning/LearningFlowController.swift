import Foundation
import Combine

enum LearningPhase {
    case pass1
    case pass2
    case cumulativeReview
    case completed
}

struct LearningState {
    var currentPhraseText: String = ""
    var isListening: Bool = false
    var feedback: String? = nil
    var isCorrect: Bool = false
    var canContinue: Bool = false
    var currentSection: Int = 0
    var totalSections: Int = 0
    var currentParagraph: Int = 0
    var totalParagraphs: Int = 0
    var currentPhrase: Int = 0
    var totalPhrases: Int = 0
}

class LearningFlowController {
    private let database: MemorizeDatabase
    private let textId: String
    private let onComplete: (String) -> Void
    
    private var currentPhase: LearningPhase = .pass1
    private var currentSectionIndex = 0
    private var currentParagraphIndex = 0
    private var currentPhraseIndex = 0
    private var sections: [SectionModel] = []
    private var paragraphs: [ParagraphModel] = []
    private var phrases: [PhraseModel] = []
    private var sessionId: String?
    private var mistakesCount = 0
    private var repetitionsCount = 0
    private var startTime: Date = Date()
    
    var onStateUpdate: ((LearningState) -> Void)?
    
    private let ttsManager = TextToSpeechManager()
    private let speechRecognition = SpeechRecognitionManager()
    
    init(database: MemorizeDatabase, textId: String, onComplete: @escaping (String) -> Void) {
        self.database = database
        self.textId = textId
        self.onComplete = onComplete
    }
    
    func initialize() async {
        let sectionRepository = SectionRepository(database: database)
        sections = sectionRepository.getSectionsByTextId(textId: textId)
        
        if sections.isEmpty {
            onStateUpdate?(LearningState())
            return
        }
        
        await loadCurrentParagraph()
        
        // Request speech recognition authorization
        await speechRecognition.requestAuthorization { _ in }
        
        // Create learning session
        sessionId = UUID().uuidString
        startTime = Date()
        let session = LearningSessionModel(
            id: sessionId!,
            textId: textId,
            startTime: startTime
        )
        do {
            try LearningSessionRepository(database: database).insertSession(session)
        } catch {
            print("Error creating session: \(error)")
        }
        
        // Start with first phrase
        await startPass1()
    }
    
    private func loadCurrentParagraph() async {
        if sections.isEmpty { return }
        
        let currentSection = sections[currentSectionIndex]
        let paragraphRepository = ParagraphRepository(database: database)
        paragraphs = paragraphRepository.getParagraphsBySectionId(sectionId: currentSection.id)
        
        if paragraphs.isEmpty { return }
        
        let currentParagraph = paragraphs[currentParagraphIndex]
        let phraseRepository = PhraseRepository(database: database)
        phrases = phraseRepository.getPhrasesByParagraphId(paragraphId: currentParagraph.id)
    }
    
    private func startPass1() async {
        if phrases.isEmpty {
            await moveToNextParagraph()
            return
        }
        
        let currentPhrase = phrases[currentPhraseIndex]
        updateProgress()
        
        // Bot reads the phrase
        onStateUpdate?(LearningState(
            currentPhraseText: currentPhrase.text,
            isListening: false,
            feedback: "Слушайте внимательно...",
            currentSection: currentSectionIndex,
            totalSections: sections.count,
            currentParagraph: currentParagraphIndex,
            totalParagraphs: paragraphs.count,
            currentPhrase: currentPhraseIndex,
            totalPhrases: phrases.count
        ))
        
        await ttsManager.speak(currentPhrase.text) { _ in }
        
        // Wait for user to repeat
        onStateUpdate?(LearningState(
            currentPhraseText: currentPhrase.text,
            isListening: true,
            feedback: "Повторите фразу",
            currentSection: currentSectionIndex,
            totalSections: sections.count,
            currentParagraph: currentParagraphIndex,
            totalParagraphs: paragraphs.count,
            currentPhrase: currentPhraseIndex,
            totalPhrases: phrases.count
        ))
        
        await withCheckedContinuation { continuation in
            speechRecognition.recognizeSpeech { userText in
                continuation.resume()
                
                if userText == nil || TextComparator.isDontRemember(userText!) {
                    self.onStateUpdate?(LearningState(
                        currentPhraseText: currentPhrase.text,
                        isListening: false,
                        feedback: "Попробуйте еще раз",
                        isCorrect: false,
                        canContinue: false,
                        currentSection: self.currentSectionIndex,
                        totalSections: self.sections.count,
                        currentParagraph: self.currentParagraphIndex,
                        totalParagraphs: self.paragraphs.count,
                        currentPhrase: self.currentPhraseIndex,
                        totalPhrases: self.phrases.count
                    ))
                    return
                }
                
                let isCorrect = TextComparator.compareTexts(original: currentPhrase.text, userText: userText!)
                
                if isCorrect {
                    self.onStateUpdate?(LearningState(
                        currentPhraseText: currentPhrase.text,
                        isListening: false,
                        feedback: "Правильно! ✓",
                        isCorrect: true,
                        canContinue: true,
                        currentSection: self.currentSectionIndex,
                        totalSections: self.sections.count,
                        currentParagraph: self.currentParagraphIndex,
                        totalParagraphs: self.paragraphs.count,
                        currentPhrase: self.currentPhraseIndex,
                        totalPhrases: self.phrases.count
                    ))
                } else {
                    self.onStateUpdate?(LearningState(
                        currentPhraseText: currentPhrase.text,
                        isListening: false,
                        feedback: "Не совсем правильно. Попробуйте еще раз.",
                        isCorrect: false,
                        canContinue: false,
                        currentSection: self.currentSectionIndex,
                        totalSections: self.sections.count,
                        currentParagraph: self.currentParagraphIndex,
                        totalParagraphs: self.paragraphs.count,
                        currentPhrase: self.currentPhraseIndex,
                        totalPhrases: self.phrases.count
                    ))
                }
            }
        }
    }
    
    func onDontRemember() async {
        mistakesCount++
        repetitionsCount++
        
        let currentPhrase = phrases[currentPhraseIndex]
        onStateUpdate?(LearningState(
            currentPhraseText: currentPhrase.text,
            isListening: false,
            feedback: "Вот правильная фраза. Повторите.",
            isCorrect: false,
            canContinue: false
        ))
        
        await ttsManager.speak(currentPhrase.text) { _ in }
    }
    
    func continueToNext() async {
        switch currentPhase {
        case .pass1:
            // Mark phrase as learned
            let currentPhrase = phrases[currentPhraseIndex]
            do {
                try PhraseRepository(database: database).updateLearnedStatus(id: currentPhrase.id, isLearned: true)
            } catch {
                print("Error updating phrase: \(error)")
            }
            
            // Move to next phrase
            currentPhraseIndex++
            repetitionsCount++
            
            if currentPhraseIndex >= phrases.count {
                // All phrases learned in pass1, move to pass2
                currentPhraseIndex = 0
                currentPhase = .pass2
                await startPass2()
            } else {
                await startPass1()
            }
            
        case .pass2:
            // Similar logic for pass2
            let currentPhrase = phrases[currentPhraseIndex]
            do {
                try PhraseRepository(database: database).updateLearnedStatus(id: currentPhrase.id, isLearned: true)
            } catch {
                print("Error updating phrase: \(error)")
            }
            
            currentPhraseIndex++
            repetitionsCount++
            
            if currentPhraseIndex >= phrases.count {
                currentPhraseIndex = 0
                currentPhase = .cumulativeReview
                await startCumulativeReview()
            } else {
                await startPass2()
            }
            
        case .cumulativeReview:
            await continueCumulativeReview()
            
        case .completed:
            break
        }
    }
    
    private func startPass2() async {
        // Similar to pass1 but user says first
        // Implementation similar to Android version
    }
    
    private func startCumulativeReview() async {
        // Implementation similar to Android version
    }
    
    private func continueCumulativeReview() async {
        await moveToNextParagraph()
    }
    
    private func moveToNextParagraph() async {
        currentParagraphIndex++
        
        if currentParagraphIndex >= paragraphs.count {
            currentParagraphIndex = 0
            currentSectionIndex++
            
            if currentSectionIndex >= sections.count {
                await completeLearning()
                return
            }
        }
        
        currentPhraseIndex = 0
        currentPhase = .pass1
        await loadCurrentParagraph()
        await startPass1()
    }
    
    private func updateProgress() {
        onStateUpdate?(LearningState(
            currentSection: currentSectionIndex,
            totalSections: sections.count,
            currentParagraph: currentParagraphIndex,
            totalParagraphs: paragraphs.count,
            currentPhrase: currentPhraseIndex,
            totalPhrases: phrases.count
        ))
    }
    
    private func completeLearning() async {
        currentPhase = .completed
        
        let endTime = Date()
        let duration = endTime.timeIntervalSince(startTime)
        
        let accuracy = repetitionsCount > 0 ? 1.0 - (Double(mistakesCount) / Double(repetitionsCount)) : 0.0
        let grade = Float(accuracy * 100)
        
        if let sessionId = sessionId {
            let session = LearningSessionRepository(database: database).getSessionById(id: sessionId)
            if var session = session {
                session = LearningSessionModel(
                    id: session.id,
                    textId: session.textId,
                    startTime: session.startTime,
                    endTime: endTime,
                    totalRepetitions: repetitionsCount,
                    mistakesCount: mistakesCount,
                    grade: grade
                )
                do {
                    try LearningSessionRepository(database: database).updateSession(session)
                } catch {
                    print("Error updating session: \(error)")
                }
            }
        }
        
        onComplete(sessionId ?? "")
    }
}

