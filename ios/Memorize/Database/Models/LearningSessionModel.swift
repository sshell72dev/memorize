import Foundation
import SQLite

struct LearningSessionModel {
    let id: String
    let textId: String
    let startTime: Date
    let endTime: Date?
    let totalRepetitions: Int
    let mistakesCount: Int
    let grade: Float?
    
    static let table = Table("learning_sessions")
    static let id = Expression<String>("id")
    static let textId = Expression<String>("textId")
    static let startTime = Expression<Int64>("startTime")
    static let endTime = Expression<Int64?>("endTime")
    static let totalRepetitions = Expression<Int>("totalRepetitions")
    static let mistakesCount = Expression<Int>("mistakesCount")
    static let grade = Expression<Double?>("grade")
    
    init(id: String, textId: String, startTime: Date, endTime: Date? = nil, totalRepetitions: Int = 0, mistakesCount: Int = 0, grade: Float? = nil) {
        self.id = id
        self.textId = textId
        self.startTime = startTime
        self.endTime = endTime
        self.totalRepetitions = totalRepetitions
        self.mistakesCount = mistakesCount
        self.grade = grade
    }
    
    init(row: Row) {
        self.id = row[LearningSessionModel.id]
        self.textId = row[LearningSessionModel.textId]
        self.startTime = Date(timeIntervalSince1970: TimeInterval(row[LearningSessionModel.startTime]))
        if let endTimeValue = row[LearningSessionModel.endTime] {
            self.endTime = Date(timeIntervalSince1970: TimeInterval(endTimeValue))
        } else {
            self.endTime = nil
        }
        self.totalRepetitions = row[LearningSessionModel.totalRepetitions]
        self.mistakesCount = row[LearningSessionModel.mistakesCount]
        if let gradeValue = row[LearningSessionModel.grade] {
            self.grade = Float(gradeValue)
        } else {
            self.grade = nil
        }
    }
}

