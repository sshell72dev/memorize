package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "texts",
    indices = [androidx.room.Index(value = ["userId", "title"])]
)
data class TextEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val fullText: String,
    val createdAt: Long = System.currentTimeMillis()
)

