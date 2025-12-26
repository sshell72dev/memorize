package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TextEntity::class,
            parentColumns = ["id"],
            childColumns = ["textId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["textId"])]
)
data class LearningSessionEntity(
    @PrimaryKey
    val id: String,
    val textId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalRepetitions: Int = 0,
    val mistakesCount: Int = 0,
    val grade: Float? = null
)

