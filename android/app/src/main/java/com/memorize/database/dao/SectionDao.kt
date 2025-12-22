package com.memorize.database.dao

import androidx.room.*
import com.memorize.database.entity.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE textId = :textId ORDER BY `order`")
    fun getSectionsByTextId(textId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getSectionById(id: String): SectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>)

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Query("UPDATE sections SET isLearned = :isLearned WHERE id = :id")
    suspend fun updateLearnedStatus(id: String, isLearned: Boolean)
}

