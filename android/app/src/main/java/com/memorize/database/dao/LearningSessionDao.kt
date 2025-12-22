package com.memorize.database.dao

import androidx.room.*
import com.memorize.database.entity.LearningSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSessionDao {
    @Query("SELECT * FROM learning_sessions WHERE textId = :textId ORDER BY startTime DESC")
    fun getSessionsByTextId(textId: String): Flow<List<LearningSessionEntity>>

    @Query("SELECT * FROM learning_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): LearningSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LearningSessionEntity)

    @Update
    suspend fun updateSession(session: LearningSessionEntity)
}

