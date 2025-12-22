package com.memorize.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.memorize.database.dao.*
import com.memorize.database.entity.*

@Database(
    entities = [
        TextEntity::class,
        SectionEntity::class,
        ParagraphEntity::class,
        PhraseEntity::class,
        LearningSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MemorizeDatabase : RoomDatabase() {
    abstract fun textDao(): TextDao
    abstract fun sectionDao(): SectionDao
    abstract fun paragraphDao(): ParagraphDao
    abstract fun phraseDao(): PhraseDao
    abstract fun learningSessionDao(): LearningSessionDao

    companion object {
        const val DATABASE_NAME = "memorize_db"
    }
}

