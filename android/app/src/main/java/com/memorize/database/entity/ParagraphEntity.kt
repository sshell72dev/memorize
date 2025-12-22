package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "paragraphs",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ParagraphEntity(
    @PrimaryKey
    val id: String,
    val sectionId: String,
    val order: Int,
    val isLearned: Boolean = false
)

