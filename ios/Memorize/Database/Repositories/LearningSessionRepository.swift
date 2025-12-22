import Foundation
import SQLite

class LearningSessionRepository {
    private let db: Connection
    
    init(database: MemorizeDatabase) {
        guard let connection = database.getConnection() else {
            fatalError("Database connection not available")
        }
        self.db = connection
    }
    
    func getSessionsByTextId(textId: String) -> [LearningSessionModel] {
        do {
            let query = LearningSessionModel.table
                .filter(LearningSessionModel.textId == textId)
                .order(LearningSessionModel.startTime.desc)
            let sessions = try db.prepare(query)
            return sessions.map { LearningSessionModel(row: $0) }
        } catch {
            print("Error getting sessions: \(error)")
            return []
        }
    }
    
    func getSessionById(id: String) -> LearningSessionModel? {
        do {
            let query = LearningSessionModel.table.filter(LearningSessionModel.id == id)
            if let row = try db.pluck(query) {
                return LearningSessionModel(row: row)
            }
        } catch {
            print("Error getting session: \(error)")
        }
        return nil
    }
    
    func insertSession(_ session: LearningSessionModel) throws {
        let insert = LearningSessionModel.table.insert(
            LearningSessionModel.id <- session.id,
            LearningSessionModel.textId <- session.textId,
            LearningSessionModel.startTime <- Int64(session.startTime.timeIntervalSince1970),
            LearningSessionModel.endTime <- session.endTime.map { Int64($0.timeIntervalSince1970) },
            LearningSessionModel.totalRepetitions <- session.totalRepetitions,
            LearningSessionModel.mistakesCount <- session.mistakesCount,
            LearningSessionModel.grade <- session.grade.map { Double($0) }
        )
        try db.run(insert)
    }
    
    func updateSession(_ session: LearningSessionModel) throws {
        let query = LearningSessionModel.table.filter(LearningSessionModel.id == session.id)
        try db.run(query.update(
            LearningSessionModel.endTime <- session.endTime.map { Int64($0.timeIntervalSince1970) },
            LearningSessionModel.totalRepetitions <- session.totalRepetitions,
            LearningSessionModel.mistakesCount <- session.mistakesCount,
            LearningSessionModel.grade <- session.grade.map { Double($0) }
        ))
    }
}

