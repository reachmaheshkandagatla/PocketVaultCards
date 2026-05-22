package com.mahesh.pocketvault.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FolderEntity::class, CardEntity::class, GroceryItemEntity::class, BankCardEntity::class], version = 9)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun cardDao(): CardDao
    abstract fun groceryItemDao(): GroceryItemDao
    abstract fun bankCardDao(): BankCardDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN kind TEXT NOT NULL DEFAULT 'cards'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grocery_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        folderId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        isDone INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cards_folderId ON cards(folderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grocery_items_folderId ON grocery_items(folderId)")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN lastFourDigits TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN pin TEXT")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bank_cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        folderId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        lastFourDigits TEXT NOT NULL,
                        pin TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bank_cards_folderId ON bank_cards(folderId)")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_cards ADD COLUMN cardType TEXT NOT NULL DEFAULT 'Debit'")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_cards ADD COLUMN usageCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE bank_cards ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bank_cards ADD COLUMN colorKey TEXT NOT NULL DEFAULT 'Blue'")
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "pocketvault.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .build()
                .also { INSTANCE = it }
        }
    }
}
