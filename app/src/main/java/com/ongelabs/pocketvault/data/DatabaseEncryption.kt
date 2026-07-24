package com.ongelabs.pocketvault.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.FileInputStream

object DatabaseEncryption {
    private const val DATABASE_NAME = "pocketvault.db"
    private const val BACKUP_SUFFIX = ".plaintext-migration"
    private val TABLES = listOf("folders", "cards", "grocery_items", "bank_cards")
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun preparePlaintextMigration(context: Context): File? {
        val database = context.getDatabasePath(DATABASE_NAME)
        val backup = File(database.parentFile, database.name + BACKUP_SUFFIX)
        if (backup.exists()) return backup
        if (!database.exists() || !hasPlaintextHeader(database)) return null

        SQLiteDatabase.openDatabase(
            database.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        ).use { source ->
            source.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        }

        check(database.renameTo(backup)) { "Unable to stage the plaintext database for encryption" }
        File(database.path + "-wal").delete()
        File(database.path + "-shm").delete()
        return backup
    }

    fun copyIntoEncryptedDatabase(backup: File, destination: SupportSQLiteDatabase) {
        if (!backup.exists()) return
        if (TABLES.any { destination.rowCount(it) > 0L }) {
            backup.delete()
            return
        }

        val source = SQLiteDatabase.openDatabase(
            backup.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            destination.beginTransaction()
            try {
                TABLES.forEach { table -> copyTable(source, destination, table) }
                destination.setTransactionSuccessful()
            } finally {
                destination.endTransaction()
            }
        } finally {
            source.close()
        }

        check(backup.delete()) { "The encrypted database was created, but its plaintext migration copy could not be removed" }
    }

    private fun copyTable(
        source: SQLiteDatabase,
        destination: SupportSQLiteDatabase,
        table: String
    ) {
        if (!source.hasTable(table)) return
        val destinationColumns = destination.tableColumns(table)
        source.rawQuery("SELECT * FROM `$table`", null).use { cursor ->
            val columns = cursor.columnNames.filter(destinationColumns::contains)
            if (columns.isEmpty()) return
            val quotedColumns = columns.joinToString(",") { "`$it`" }
            val placeholders = List(columns.size) { "?" }.joinToString(",")
            val sql = "INSERT INTO `$table` ($quotedColumns) VALUES ($placeholders)"
            while (cursor.moveToNext()) {
                val values = columns.map { cursor.valueAt(cursor.getColumnIndexOrThrow(it)) }.toTypedArray()
                destination.execSQL(sql, values)
            }
        }
    }

    private fun hasPlaintextHeader(file: File): Boolean {
        val header = ByteArray(SQLITE_HEADER.size)
        val count = FileInputStream(file).use { it.read(header) }
        return count == SQLITE_HEADER.size && header.contentEquals(SQLITE_HEADER)
    }

    private fun SQLiteDatabase.hasTable(table: String): Boolean {
        return rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = ? AND name = ? LIMIT 1",
            arrayOf("table", table)
        ).use { it.moveToFirst() }
    }

    private fun SupportSQLiteDatabase.rowCount(table: String): Long {
        return query("SELECT COUNT(*) FROM `$table`").use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    private fun SupportSQLiteDatabase.tableColumns(table: String): Set<String> {
        return query("PRAGMA table_info(`$table`)").use { cursor ->
            buildSet {
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
    }

    private fun Cursor.valueAt(index: Int): Any? = when (getType(index)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_INTEGER -> getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
        Cursor.FIELD_TYPE_STRING -> getString(index)
        Cursor.FIELD_TYPE_BLOB -> getBlob(index)
        else -> error("Unsupported SQLite value type")
    }
}
