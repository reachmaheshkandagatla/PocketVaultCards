package com.mahesh.pocketvault.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, usageCount DESC, lastOpenedAt DESC")
    fun cardsByFolder(folderId: Long): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE folderId = :folderId AND isDeleted = 0")
    fun countByFolder(folderId: Long): Flow<Int>

    @Query("SELECT * FROM cards WHERE isDeleted = 1 AND folderId = :folderId ORDER BY deletedAt DESC")
    fun deletedCardsByFolder(folderId: Long): Flow<List<CardEntity>>

    @Query("SELECT COUNT(*) FROM cards WHERE isDeleted = 1 AND folderId = :folderId")
    fun countDeletedByFolder(folderId: Long): Flow<Int>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CardEntity?

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)
}
