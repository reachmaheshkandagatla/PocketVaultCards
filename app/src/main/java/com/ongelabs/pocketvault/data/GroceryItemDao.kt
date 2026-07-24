package com.ongelabs.pocketvault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryItemDao {
    @Query("SELECT * FROM grocery_items WHERE folderId = :folderId ORDER BY isDone ASC, createdAt ASC")
    fun itemsByFolder(folderId: Long): Flow<List<GroceryItemEntity>>

    @Query("SELECT COUNT(*) FROM grocery_items WHERE folderId = :folderId")
    fun countByFolder(folderId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM grocery_items WHERE folderId = :folderId AND isDone = 0")
    suspend fun pendingCountByFolder(folderId: Long): Int

    @Insert
    suspend fun insert(item: GroceryItemEntity): Long

    @Update
    suspend fun update(item: GroceryItemEntity)

    @Delete
    suspend fun delete(item: GroceryItemEntity)
}
