package com.memorize.database.dao

import androidx.room.*
import com.memorize.database.entity.TextEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextDao {
    @Query("SELECT * FROM texts WHERE title LIKE '%' || :query || '%'")
    fun searchTexts(query: String): Flow<List<TextEntity>>

    @Query("SELECT * FROM texts WHERE id = :id")
    suspend fun getTextById(id: String): TextEntity?

    @Query("SELECT * FROM texts ORDER BY createdAt DESC")
    fun getAllTexts(): Flow<List<TextEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertText(text: TextEntity)

    @Delete
    suspend fun deleteText(text: TextEntity)
}

