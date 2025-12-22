import Foundation
import SQLite

class ParagraphRepository {
    private let db: Connection
    
    init(database: MemorizeDatabase) {
        guard let connection = database.getConnection() else {
            fatalError("Database connection not available")
        }
        self.db = connection
    }
    
    func getParagraphsBySectionId(sectionId: String) -> [ParagraphModel] {
        do {
            let query = ParagraphModel.table
                .filter(ParagraphModel.sectionId == sectionId)
                .order(ParagraphModel.order)
            let paragraphs = try db.prepare(query)
            return paragraphs.map { ParagraphModel(row: $0) }
        } catch {
            print("Error getting paragraphs: \(error)")
            return []
        }
    }
    
    func getParagraphById(id: String) -> ParagraphModel? {
        do {
            let query = ParagraphModel.table.filter(ParagraphModel.id == id)
            if let row = try db.pluck(query) {
                return ParagraphModel(row: row)
            }
        } catch {
            print("Error getting paragraph: \(error)")
        }
        return nil
    }
    
    func insertParagraph(_ paragraph: ParagraphModel) throws {
        let insert = ParagraphModel.table.insert(
            ParagraphModel.id <- paragraph.id,
            ParagraphModel.sectionId <- paragraph.sectionId,
            ParagraphModel.order <- paragraph.order,
            ParagraphModel.isLearned <- paragraph.isLearned
        )
        try db.run(insert)
    }
    
    func insertParagraphs(_ paragraphs: [ParagraphModel]) throws {
        for paragraph in paragraphs {
            try insertParagraph(paragraph)
        }
    }
    
    func updateLearnedStatus(id: String, isLearned: Bool) throws {
        let query = ParagraphModel.table.filter(ParagraphModel.id == id)
        try db.run(query.update(ParagraphModel.isLearned <- isLearned))
    }
}

