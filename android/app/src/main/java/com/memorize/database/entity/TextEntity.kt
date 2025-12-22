package com.memorize.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "texts")
data class TextEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val fullText: String,
    val createdAt: Long = System.currentTimeMillis()
)

