import Foundation
import SQLite

struct TextModel {
    let id: String
    let title: String
    let fullText: String
    let createdAt: Date
    
    static let table = Table("texts")
    static let id = Expression<String>("id")
    static let title = Expression<String>("title")
    static let fullText = Expression<String>("fullText")
    static let createdAt = Expression<Int64>("createdAt")
    
    init(id: String, title: String, fullText: String, createdAt: Date = Date()) {
        self.id = id
        self.title = title
        self.fullText = fullText
        self.createdAt = createdAt
    }
    
    init(row: Row) {
        self.id = row[TextModel.id]
        self.title = row[TextModel.title]
        self.fullText = row[TextModel.fullText]
        self.createdAt = Date(timeIntervalSince1970: TimeInterval(row[TextModel.createdAt]))
    }
}

