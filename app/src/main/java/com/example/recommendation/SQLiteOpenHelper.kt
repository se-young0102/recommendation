package com.example.recommendation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

// 책 정보를 담는 데이터 클래스
data class BookItem(
    val id: Int,
    val title: String,
    val author: String,
    val publisher: String?,
    val content: String?,
    val coverUri: String?
)

// SQLiteOpenHelper를 상속받아 DB 생성 및 관리
class BookDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "books.db" // DB 이름
        private const val DATABASE_VERSION = 1        // DB 버전

        // 테이블 및 컬럼 상수
        const val TABLE_NAME = "books"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_AUTHOR = "author"
        const val COLUMN_PUBLISHER = "publisher"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_COVER_URI = "cover_uri"
    }

    // DB 처음 생성될 때 호출
    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_AUTHOR TEXT NOT NULL,
                $COLUMN_PUBLISHER TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_COVER_URI TEXT
            )
        """.trimIndent()
        db.execSQL(createTableStatement)
    }

    // DB 업그레이드 시 기존 테이블 삭제 후 재생성
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 책 정보 삽입
    fun insertBook(title: String, author: String, publisher: String?, content: String?, coverUri: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_AUTHOR, author)
            put(COLUMN_PUBLISHER, publisher)
            put(COLUMN_CONTENT, content)
            put(COLUMN_COVER_URI, coverUri)
        }
        return db.insert(TABLE_NAME, null, values) != -1L
    }

    // 책 정보 수정
    fun updateBook(id: Int, title: String, author: String, publisher: String?, content: String?, coverUri: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_AUTHOR, author)
            put(COLUMN_PUBLISHER, publisher)
            put(COLUMN_CONTENT, content)
            put(COLUMN_COVER_URI, coverUri)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
    }

    // 책 삭제
    fun deleteBook(id: Int): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString())) > 0
    }

    // 전체 책 목록 가져오기 (최신순 정렬)
    fun getAllBooks(): List<BookItem> {
        val bookList = mutableListOf<BookItem>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null, null, null, null, null,
            "$COLUMN_ID DESC"
        )
        if (cursor.moveToFirst()) {
            do {
                // 컬럼 값 추출
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR))
                val publisher = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUBLISHER))
                val content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT))
                val coverUri = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_URI))

                // 리스트에 추가
                bookList.add(BookItem(id, title, author, publisher, content, coverUri))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return bookList
    }
}
