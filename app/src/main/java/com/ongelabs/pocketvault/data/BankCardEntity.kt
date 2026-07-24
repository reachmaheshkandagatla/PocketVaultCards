package com.ongelabs.pocketvault.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_cards",
    indices = [Index("folderId")],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BankCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val cardType: String = TYPE_DEBIT,
    val colorKey: String = COLOR_BLUE,
    val lastFourDigits: String,
    val pin: String,
    val usageCount: Int = 0,
    val lastViewedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CREDIT = "Credit"
        const val TYPE_DEBIT = "Debit"
        const val COLOR_BLUE = "Blue"
        const val COLOR_GREEN = "Green"
        const val COLOR_RED = "Red"
        const val COLOR_GOLD = "Gold"
        const val COLOR_BLACK = "Black"
    }
}
