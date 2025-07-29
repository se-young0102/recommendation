package com.example.recommendation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: BookDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = BookDatabaseHelper(this)

        // 기록하기 버튼
        findViewById<Button>(R.id.button).setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        // 폴더 클릭 시
        findViewById<ImageView>(R.id.folder1).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }
        findViewById<ImageView>(R.id.folder2).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }

        // 도서추천 버튼 (추후 기능 추가)
        findViewById<Button>(R.id.button3).setOnClickListener {
            // TODO: 추천 기능 추가
        }

        // 내가 읽은 도서 영역에 DB에서 제목 넣기
        val bookList = dbHelper.getAllBooks().sortedByDescending { it.id } // 최신순 정렬

        val titles = bookList.take(3).map { it.title } // 최대 3개까지만

        findViewById<TextView>(R.id.bookAText).text = titles.getOrNull(0) ?: "도서 없음"
        findViewById<TextView>(R.id.bookBText).text = titles.getOrNull(1) ?: ""
        findViewById<TextView>(R.id.bookCText).text = titles.getOrNull(2) ?: ""
    }
}
