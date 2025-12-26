import Foundation

class TextService {
    private let database: MemorizeDatabase
    private let yandexGPTService: YandexGPTService
    private let textRepository: TextRepository
    private let sectionRepository: SectionRepository
    private let paragraphRepository: ParagraphRepository
    private let phraseRepository: PhraseRepository
    
    init(
        database: MemorizeDatabase,
        yandexGPTService: YandexGPTService
    ) {
        self.database = database
        self.yandexGPTService = yandexGPTService
        self.textRepository = TextRepository(database: database)
        self.sectionRepository = SectionRepository(database: database)
        self.paragraphRepository = ParagraphRepository(database: database)
        self.phraseRepository = PhraseRepository(database: database)
    }
    
    func getTextByTitle(title: String, author: String? = nil) async throws -> String? {
        // Get text from Yandex GPT without saving
        return try await yandexGPTService.getTextByTitle(title: title, author: author)
    }
    
    func loadAndSaveText(title: String, author: String? = nil) async throws -> String? {
        // Check if text already exists
        if let existingText = textRepository.getTextById(id: title) {
            return existingText.id
        }
        
        // Get text from Yandex GPT
        guard let fullText = try await yandexGPTService.getTextByTitle(title: title, author: author) else {
            return nil
        }
        
        // Parse text
        guard let parsedStructure = try await yandexGPTService.parseText(fullText) else {
            return nil
        }
        
        // Save to database
        let textId = UUID().uuidString
        let textModel = TextModel(
            id: textId,
            title: title,
            fullText: fullText
        )
        try textRepository.insertText(textModel)
        
        // Save sections, paragraphs, and phrases
        for (sectionIndex, section) in parsedStructure.sections.enumerated() {
            let sectionId = UUID().uuidString
            let sectionModel = SectionModel(
                id: sectionId,
                textId: textId,
                order: sectionIndex
            )
            try sectionRepository.insertSection(sectionModel)
            
            for (paragraphIndex, paragraph) in section.paragraphs.enumerated() {
                let paragraphId = UUID().uuidString
                let paragraphModel = ParagraphModel(
                    id: paragraphId,
                    sectionId: sectionId,
                    order: paragraphIndex
                )
                try paragraphRepository.insertParagraph(paragraphModel)
                
                for (phraseIndex, phraseText) in paragraph.phrases.enumerated() {
                    let phraseId = UUID().uuidString
                    let phraseModel = PhraseModel(
                        id: phraseId,
                        paragraphId: paragraphId,
                        order: phraseIndex,
                        text: phraseText
                    )
                    try phraseRepository.insertPhrase(phraseModel)
                }
            }
        }
        
        return textId
    }
    
    func saveTextDirectly(title: String, fullText: String) async throws -> String? {
        // Check if text already exists
        if let existingText = textRepository.getTextById(id: title) {
            return existingText.id
        }
        
        // Parse text using AI
        guard let parsedStructure = try await yandexGPTService.parseText(fullText) else {
            return nil
        }
        
        // Save to database
        let textId = UUID().uuidString
        let textModel = TextModel(
            id: textId,
            title: title,
            fullText: fullText
        )
        try textRepository.insertText(textModel)
        
        // Save sections, paragraphs, and phrases
        for (sectionIndex, section) in parsedStructure.sections.enumerated() {
            let sectionId = UUID().uuidString
            let sectionModel = SectionModel(
                id: sectionId,
                textId: textId,
                order: sectionIndex
            )
            try sectionRepository.insertSection(sectionModel)
            
            for (paragraphIndex, paragraph) in section.paragraphs.enumerated() {
                let paragraphId = UUID().uuidString
                let paragraphModel = ParagraphModel(
                    id: paragraphId,
                    sectionId: sectionId,
                    order: paragraphIndex
                )
                try paragraphRepository.insertParagraph(paragraphModel)
                
                for (phraseIndex, phraseText) in paragraph.phrases.enumerated() {
                    let phraseId = UUID().uuidString
                    let phraseModel = PhraseModel(
                        id: phraseId,
                        paragraphId: paragraphId,
                        order: phraseIndex,
                        text: phraseText
                    )
                    try phraseRepository.insertPhrase(phraseModel)
                }
            }
        }
        
        return textId
    }
}

