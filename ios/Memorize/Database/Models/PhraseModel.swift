import Foundation
import SQLite

struct PhraseModel {
    let id: String
    let paragraphId: String
    let order: Int
    let text: String
    let isLearned: Bool
    
    static let table = Table("phrases")
    static let id = Expression<String>("id")
    static let paragraphId = Expression<String>("paragraphId")
    static let order = Expression<Int>("order")
    static let text = Expression<String>("text")
    static let isLearned = Expression<Bool>("isLearned")
    
    init(id: String, paragraphId: String, order: Int, text: String, isLearned: Bool = false) {
        self.id = id
        self.paragraphId = paragraphId
        self.order = order
        self.text = text
        self.isLearned = isLearned
    }
    
    init(row: Row) {
        self.id = row[PhraseModel.id]
        self.paragraphId = row[PhraseModel.paragraphId]
        self.order = row[PhraseModel.order]
        self.text = row[PhraseModel.text]
        self.isLearned = row[PhraseModel.isLearned]
    }
}

