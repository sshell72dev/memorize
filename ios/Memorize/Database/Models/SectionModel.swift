import Foundation
import SQLite

struct SectionModel {
    let id: String
    let textId: String
    let order: Int
    let isLearned: Bool
    
    static let table = Table("sections")
    static let id = Expression<String>("id")
    static let textId = Expression<String>("textId")
    static let order = Expression<Int>("order")
    static let isLearned = Expression<Bool>("isLearned")
    
    init(id: String, textId: String, order: Int, isLearned: Bool = false) {
        self.id = id
        self.textId = textId
        self.order = order
        self.isLearned = isLearned
    }
    
    init(row: Row) {
        self.id = row[SectionModel.id]
        self.textId = row[SectionModel.textId]
        self.order = row[SectionModel.order]
        self.isLearned = row[SectionModel.isLearned]
    }
}

