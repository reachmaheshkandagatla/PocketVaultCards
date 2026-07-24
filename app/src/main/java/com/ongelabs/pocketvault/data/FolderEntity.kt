package com.ongelabs.pocketvault.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "ID",
    @ColumnInfo(defaultValue = "'cards'") val kind: String = KIND_CARDS,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val KIND_CARDS = "cards"
        const val KIND_COUPONS = "coupons"
        const val KIND_GROCERIES = "groceries"
        const val KIND_BANK_CARDS = "bank_cards"
        const val KIND_BILLS = "bills"
    }
}
