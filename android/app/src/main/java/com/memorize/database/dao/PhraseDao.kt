package com.memorize.database.dao

import androidx.room.*
import com.memorize.database.entity.PhraseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhraseDao {
    @Query("SELECT * FROM phrases WHERE paragraphId = :paragraphId ORDER BY `order`")
    fun getPhrasesByParagraphId(paragraphId: String): Flow<List<PhraseEntity>>

    @Query("SELECT * FROM phrases WHERE id = :id")
    suspend fun getPhraseById(id: String): PhraseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PhraseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrases(phrases: List<PhraseEntity>)

    @Update
    suspend fun updatePhrase(phrase: PhraseEntity)

    @Query("UPDATE phrases SET isLearned = :isLearned WHERE id = :id")
    suspend fun updateLearnedStatus(id: String, isLearned: Boolean)
}

