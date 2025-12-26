package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sections",
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
data class SectionEntity(
    @PrimaryKey
    val id: String,
    val textId: String,
    val order: Int,
    val isLearned: Boolean = false
)

