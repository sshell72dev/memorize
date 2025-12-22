import Foundation
import SQLite

class SectionRepository {
    private let db: Connection
    
    init(database: MemorizeDatabase) {
        guard let connection = database.getConnection() else {
            fatalError("Database connection not available")
        }
        self.db = connection
    }
    
    func getSectionsByTextId(textId: String) -> [SectionModel] {
        do {
            let query = SectionModel.table
                .filter(SectionModel.textId == textId)
                .order(SectionModel.order)
            let sections = try db.prepare(query)
            return sections.map { SectionModel(row: $0) }
        } catch {
            print("Error getting sections: \(error)")
            return []
        }
    }
    
    func getSectionById(id: String) -> SectionModel? {
        do {
            let query = SectionModel.table.filter(SectionModel.id == id)
            if let row = try db.pluck(query) {
                return SectionModel(row: row)
            }
        } catch {
            print("Error getting section: \(error)")
        }
        return nil
    }
    
    func insertSection(_ section: SectionModel) throws {
        let insert = SectionModel.table.insert(
            SectionModel.id <- section.id,
            SectionModel.textId <- section.textId,
            SectionModel.order <- section.order,
            SectionModel.isLearned <- section.isLearned
        )
        try db.run(insert)
    }
    
    func insertSections(_ sections: [SectionModel]) throws {
        for section in sections {
            try insertSection(section)
        }
    }
    
    func updateLearnedStatus(id: String, isLearned: Bool) throws {
        let query = SectionModel.table.filter(SectionModel.id == id)
        try db.run(query.update(SectionModel.isLearned <- isLearned))
    }
}

