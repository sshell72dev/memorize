import Foundation
import SQLite

class PhraseRepository {
    private let db: Connection
    
    init(database: MemorizeDatabase) {
        guard let connection = database.getConnection() else {
            fatalError("Database connection not available")
        }
        self.db = connection
    }
    
    func getPhrasesByParagraphId(paragraphId: String) -> [PhraseModel] {
        do {
            let query = PhraseModel.table
                .filter(PhraseModel.paragraphId == paragraphId)
                .order(PhraseModel.order)
            let phrases = try db.prepare(query)
            return phrases.map { PhraseModel(row: $0) }
        } catch {
            print("Error getting phrases: \(error)")
            return []
        }
    }
    
    func getPhraseById(id: String) -> PhraseModel? {
        do {
            let query = PhraseModel.table.filter(PhraseModel.id == id)
            if let row = try db.pluck(query) {
                return PhraseModel(row: row)
            }
        } catch {
            print("Error getting phrase: \(error)")
        }
        return nil
    }
    
    func insertPhrase(_ phrase: PhraseModel) throws {
        let insert = PhraseModel.table.insert(
            PhraseModel.id <- phrase.id,
            PhraseModel.paragraphId <- phrase.paragraphId,
            PhraseModel.order <- phrase.order,
            PhraseModel.text <- phrase.text,
            PhraseModel.isLearned <- phrase.isLearned
        )
        try db.run(insert)
    }
    
    func insertPhrases(_ phrases: [PhraseModel]) throws {
        for phrase in phrases {
            try insertPhrase(phrase)
        }
    }
    
    func updateLearnedStatus(id: String, isLearned: Bool) throws {
        let query = PhraseModel.table.filter(PhraseModel.id == id)
        try db.run(query.update(PhraseModel.isLearned <- isLearned))
    }
}

