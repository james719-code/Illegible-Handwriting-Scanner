package com.enon.writingai.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.enon.writingai.data.local.db.dao.HistoryDao
import com.enon.writingai.data.local.db.entity.ScanResultEntity

class AppDatabase(context: Context) {
    private val historyDbHelper = HistoryDbHelper(context.applicationContext)

    val historyDao: HistoryDao = SQLiteHistoryDao(historyDbHelper)
}

private class HistoryDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE history (
                id TEXT PRIMARY KEY,
                source_name TEXT NOT NULL,
                raw_text TEXT NOT NULL,
                normalized_text TEXT NOT NULL,
                confidence REAL NOT NULL,
                flagged_regions INTEGER NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                source_uri TEXT
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }
}

private class SQLiteHistoryDao(
    private val dbHelper: HistoryDbHelper,
) : HistoryDao {
    override fun getAll(): List<ScanResultEntity> {
        val database = dbHelper.readableDatabase
        val cursor = database.query(
            "history",
            arrayOf(
                "id",
                "source_name",
                "raw_text",
                "normalized_text",
                "confidence",
                "flagged_regions",
                "status",
                "created_at",
                "source_uri",
            ),
            null,
            null,
            null,
            null,
            "created_at DESC, rowid DESC",
        )

        return cursor.use {
            val results = mutableListOf<ScanResultEntity>()
            while (it.moveToNext()) {
                results.add(
                    ScanResultEntity(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        sourceName = it.getString(it.getColumnIndexOrThrow("source_name")),
                        rawText = it.getString(it.getColumnIndexOrThrow("raw_text")),
                        normalizedText = it.getString(it.getColumnIndexOrThrow("normalized_text")),
                        confidence = it.getFloat(it.getColumnIndexOrThrow("confidence")),
                        flaggedRegions = it.getInt(it.getColumnIndexOrThrow("flagged_regions")),
                        status = it.getString(it.getColumnIndexOrThrow("status")),
                        createdAt = it.getString(it.getColumnIndexOrThrow("created_at")),
                        sourceUri = it.getString(it.getColumnIndexOrThrow("source_uri")),
                    ),
                )
            }
            results
        }
    }

    override fun upsert(entity: ScanResultEntity) {
        val contentValues = ContentValues().apply {
            put("id", entity.id)
            put("source_name", entity.sourceName)
            put("raw_text", entity.rawText)
            put("normalized_text", entity.normalizedText)
            put("confidence", entity.confidence)
            put("flagged_regions", entity.flaggedRegions)
            put("status", entity.status)
            put("created_at", entity.createdAt)
            put("source_uri", entity.sourceUri)
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            "history",
            null,
            contentValues,
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    override fun delete(id: String) {
        dbHelper.writableDatabase.delete(
            "history",
            "id = ?",
            arrayOf(id),
        )
    }
}

private const val DATABASE_NAME = "writing_ai.db"
private const val DATABASE_VERSION = 1
