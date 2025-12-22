import Foundation
import SQLite

struct ParagraphModel {
    let id: String
    let sectionId: String
    let order: Int
    let isLearned: Bool
    
    static let table = Table("paragraphs")
    static let id = Expression<String>("id")
    static let sectionId = Expression<String>("sectionId")
    static let order = Expression<Int>("order")
    static let isLearned = Expression<Bool>("isLearned")
    
    init(id: String, sectionId: String, order: Int, isLearned: Bool = false) {
        self.id = id
        self.sectionId = sectionId
        self.order = order
        self.isLearned = isLearned
    }
    
    init(row: Row) {
        self.id = row[ParagraphModel.id]
        self.sectionId = row[ParagraphModel.sectionId]
        self.order = row[ParagraphModel.order]
        self.isLearned = row[ParagraphModel.isLearned]
    }
}

