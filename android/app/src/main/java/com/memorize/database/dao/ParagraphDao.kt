package com.memorize.database.dao

import androidx.room.*
import com.memorize.database.entity.ParagraphEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParagraphDao {
    @Query("SELECT * FROM paragraphs WHERE sectionId = :sectionId ORDER BY `order`")
    fun getParagraphsBySectionId(sectionId: String): Flow<List<ParagraphEntity>>

    @Query("SELECT * FROM paragraphs WHERE id = :id")
    suspend fun getParagraphById(id: String): ParagraphEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraph(paragraph: ParagraphEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraphs(paragraphs: List<ParagraphEntity>)

    @Update
    suspend fun updateParagraph(paragraph: ParagraphEntity)

    @Query("UPDATE paragraphs SET isLearned = :isLearned WHERE id = :id")
    suspend fun updateLearnedStatus(id: String, isLearned: Boolean)
}

