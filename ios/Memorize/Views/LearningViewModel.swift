import Foundation
import SwiftUI

class LearningViewModel: ObservableObject {
    @Published var currentPhraseText: String = ""
    @Published var isListening: Bool = false
    @Published var feedback: String? = nil
    @Published var isCorrect: Bool = false
    @Published var canContinue: Bool = false
    @Published var currentSection: Int = 0
    @Published var totalSections: Int = 0
    @Published var currentParagraph: Int = 0
    @Published var totalParagraphs: Int = 0
    @Published var currentPhrase: Int = 0
    @Published var totalPhrases: Int = 0
    
    private let learningController: LearningFlowController
    private let textId: String
    private let onComplete: (String) -> Void
    
    init(textId: String, onComplete: @escaping (String) -> Void) {
        self.textId = textId
        self.onComplete = onComplete
        
        let database = MemorizeDatabase()
        self.learningController = LearningFlowController(
            database: database,
            textId: textId,
            onComplete: onComplete
        )
        
        learningController.onStateUpdate = { [weak self] state in
            DispatchQueue.main.async {
                self?.updateState(state)
            }
        }
        
        Task {
            await learningController.initialize()
        }
    }
    
    func onDontRemember() {
        Task {
            await learningController.onDontRemember()
        }
    }
    
    func continueToNext() {
        Task {
            await learningController.continueToNext()
        }
    }
    
    private func updateState(_ state: LearningState) {
        currentPhraseText = state.currentPhraseText
        isListening = state.isListening
        feedback = state.feedback
        isCorrect = state.isCorrect
        canContinue = state.canContinue
        currentSection = state.currentSection
        totalSections = state.totalSections
        currentParagraph = state.currentParagraph
        totalParagraphs = state.totalParagraphs
        currentPhrase = state.currentPhrase
        totalPhrases = state.totalPhrases
    }
}

