import Foundation
import SQLite

class MemorizeDatabase {
    private var db: Connection?
    
    init() {
        setupDatabase()
    }
    
    private func setupDatabase() {
        let path = NSSearchPathForDirectoriesInDomains(
            .documentDirectory, .userDomainMask, true
        ).first!
        
        do {
            db = try Connection("\(path)/memorize.db")
            createTables()
        } catch {
            print("Error creating database: \(error)")
        }
    }
    
    private func createTables() {
        guard let db = db else { return }
        
        do {
            // Texts table
            try db.run(TextModel.table.create(ifNotExists: true) { t in
                t.column(TextModel.id, primaryKey: true)
                t.column(TextModel.title)
                t.column(TextModel.fullText)
                t.column(TextModel.createdAt)
            })
            
            // Sections table
            try db.run(SectionModel.table.create(ifNotExists: true) { t in
                t.column(SectionModel.id, primaryKey: true)
                t.column(SectionModel.textId)
                t.column(SectionModel.order)
                t.column(SectionModel.isLearned, defaultValue: false)
                t.foreignKey(SectionModel.textId, references: TextModel.table, TextModel.id, delete: .cascade)
            })
            
            // Paragraphs table
            try db.run(ParagraphModel.table.create(ifNotExists: true) { t in
                t.column(ParagraphModel.id, primaryKey: true)
                t.column(ParagraphModel.sectionId)
                t.column(ParagraphModel.order)
                t.column(ParagraphModel.isLearned, defaultValue: false)
                t.foreignKey(ParagraphModel.sectionId, references: SectionModel.table, SectionModel.id, delete: .cascade)
            })
            
            // Phrases table
            try db.run(PhraseModel.table.create(ifNotExists: true) { t in
                t.column(PhraseModel.id, primaryKey: true)
                t.column(PhraseModel.paragraphId)
                t.column(PhraseModel.order)
                t.column(PhraseModel.text)
                t.column(PhraseModel.isLearned, defaultValue: false)
                t.foreignKey(PhraseModel.paragraphId, references: ParagraphModel.table, ParagraphModel.id, delete: .cascade)
            })
            
            // Learning sessions table
            try db.run(LearningSessionModel.table.create(ifNotExists: true) { t in
                t.column(LearningSessionModel.id, primaryKey: true)
                t.column(LearningSessionModel.textId)
                t.column(LearningSessionModel.startTime)
                t.column(LearningSessionModel.endTime)
                t.column(LearningSessionModel.totalRepetitions, defaultValue: 0)
                t.column(LearningSessionModel.mistakesCount, defaultValue: 0)
                t.column(LearningSessionModel.grade)
                t.foreignKey(LearningSessionModel.textId, references: TextModel.table, TextModel.id, delete: .cascade)
            })
        } catch {
            print("Error creating tables: \(error)")
        }
    }
    
    func getConnection() -> Connection? {
        return db
    }
}

