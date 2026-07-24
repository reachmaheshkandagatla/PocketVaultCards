package com.ongelabs.pocketvault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BankCardDao {
    @Query("SELECT * FROM bank_cards WHERE folderId = :folderId ORDER BY usageCount DESC, lastViewedAt DESC, createdAt DESC")
    fun cardsByFolder(folderId: Long): Flow<List<BankCardEntity>>

    @Query("SELECT COUNT(*) FROM bank_cards WHERE folderId = :folderId")
    fun countByFolder(folderId: Long): Flow<Int>

    @Insert
    suspend fun insert(card: BankCardEntity): Long

    @Update
    suspend fun update(card: BankCardEntity)

    @Delete
    suspend fun delete(card: BankCardEntity)
}
