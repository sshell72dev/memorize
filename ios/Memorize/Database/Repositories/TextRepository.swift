import Foundation
import SQLite

class TextRepository {
    private let db: Connection
    
    init(database: MemorizeDatabase) {
        guard let connection = database.getConnection() else {
            fatalError("Database connection not available")
        }
        self.db = connection
    }
    
    func searchTexts(query: String) -> [TextModel] {
        do {
            let queryFilter = TextModel.title.like("%\(query)%")
            let texts = try db.prepare(TextModel.table.filter(queryFilter))
            return texts.map { TextModel(row: $0) }
        } catch {
            print("Error searching texts: \(error)")
            return []
        }
    }
    
    func getTextById(id: String) -> TextModel? {
        do {
            let query = TextModel.table.filter(TextModel.id == id)
            if let row = try db.pluck(query) {
                return TextModel(row: row)
            }
        } catch {
            print("Error getting text: \(error)")
        }
        return nil
    }
    
    func getAllTexts() -> [TextModel] {
        do {
            let texts = try db.prepare(TextModel.table.order(TextModel.createdAt.desc))
            return texts.map { TextModel(row: $0) }
        } catch {
            print("Error getting all texts: \(error)")
            return []
        }
    }
    
    func insertText(_ text: TextModel) throws {
        let insert = TextModel.table.insert(
            TextModel.id <- text.id,
            TextModel.title <- text.title,
            TextModel.fullText <- text.fullText,
            TextModel.createdAt <- Int64(text.createdAt.timeIntervalSince1970)
        )
        try db.run(insert)
    }
    
    func deleteText(_ text: TextModel) throws {
        let query = TextModel.table.filter(TextModel.id == text.id)
        try db.run(query.delete())
    }
}

