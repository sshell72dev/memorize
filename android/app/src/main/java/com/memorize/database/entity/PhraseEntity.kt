package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "phrases",
    foreignKeys = [
        ForeignKey(
            entity = ParagraphEntity::class,
            parentColumns = ["id"],
            childColumns = ["paragraphId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["paragraphId"])]
)
data class PhraseEntity(
    @PrimaryKey
    val id: String,
    val paragraphId: String,
    val order: Int,
    val text: String,
    val isLearned: Boolean = false
)

