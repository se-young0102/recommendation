package com.example.recommendation

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.recommendation.BookDatabaseHelper

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

        // 도서추천 버튼
        findViewById<Button>(R.id.button3).setOnClickListener {
            startActivity(Intent(this, RecommendActivity::class.java))
        }

        // 폴더 클릭 시 BookData 이동
        findViewById<ImageView>(R.id.folder1).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }
        findViewById<ImageView>(R.id.folder2).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }

        // DB에서 책 리스트 가져오기 (최신순)
        val bookList = dbHelper.getAllBooks().sortedByDescending { it.id }

        // 텍스트뷰에 책 제목 세팅
        findViewById<TextView>(R.id.bookAText).text = bookList.getOrNull(0)?.title ?: "도서 없음"
        findViewById<TextView>(R.id.bookBText).text = bookList.getOrNull(1)?.title ?: ""
        findViewById<TextView>(R.id.bookCText).text = bookList.getOrNull(2)?.title ?: ""

        // 책 이미지 클릭 시 팝업 표시
        findViewById<ImageView>(R.id.bookA).setOnClickListener {
            if (bookList.size >= 1) {
                showBookDetailDialog(bookList[0])
            } else {
                showNoBookDialog()
            }
        }

        findViewById<ImageView>(R.id.bookB).setOnClickListener {
            if (bookList.size >= 2) {
                showBookDetailDialog(bookList[1])
            } else {
                showNoBookDialog()
            }
        }

        findViewById<ImageView>(R.id.bookC).setOnClickListener {
            if (bookList.size >= 3) {
                showBookDetailDialog(bookList[2])
            } else {
                showNoBookDialog()
            }
        }
    }

    // 도서 정보 팝업 다이얼로그
    private fun showBookDetailDialog(book: BookItem) {
        val message = """
            📖 제목: ${book.title}
            ✍ 저자: ${book.author}
            🏢 출판사: ${book.publisher ?: "정보 없음"}
            
            📝 메모/느낀 점:
            ${book.content ?: "작성된 내용 없음"}
        """.trimIndent()

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("도서 정보")
            .setMessage(message)
            .setPositiveButton("닫기", null)
            .show()
    }

    // 도서 없음 안내 팝업
    private fun showNoBookDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("도서 없음")
            .setMessage("해당 도서 정보가 존재하지 않습니다.")
            .setPositiveButton("확인", null)
            .show()
    }
}
