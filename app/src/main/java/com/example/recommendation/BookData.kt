package com.example.recommendation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class BookData : AppCompatActivity() {

    private lateinit var dbHelper: BookDatabaseHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.check)

        dbHelper = BookDatabaseHelper(this)

        val buttonHome = findViewById<Button>(R.id.button_home)
        buttonHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }

        val container = findViewById<LinearLayout>(R.id.containerBookList)
        container.removeAllViews()

        val bookList = dbHelper.getAllBooks()
        val inflater = LayoutInflater.from(this)

        for (book in bookList) {
            val itemView = inflater.inflate(R.layout.item_book, container, false)

            val imageView = itemView.findViewById<ImageView>(R.id.imageViewCoverItem)
            val textTitle = itemView.findViewById<TextView>(R.id.textTitleItem)
            val textAuthor = itemView.findViewById<TextView>(R.id.textAuthorItem)
            val textPublisher = itemView.findViewById<TextView>(R.id.textPublisherItem)
            val buttonEdit = itemView.findViewById<Button>(R.id.buttonEdit)
            val buttonDelete = itemView.findViewById<Button>(R.id.buttonDelete)

            textTitle.text = book.title
            textAuthor.text = book.author
            textPublisher.text = book.publisher ?: "출판사 정보 없음"

            // 이미지 Glide로 불러오기
            if (!book.coverUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(book.coverUri)
                    Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.baseline_book_24)
                        .error(R.drawable.baseline_book_24)
                        .into(imageView)

                    Log.d("BookData", "이미지 로딩 성공: $uri")
                } catch (e: Exception) {
                    Log.e("BookData", "이미지 로딩 실패: ${book.coverUri}", e)
                    imageView.setImageResource(R.drawable.baseline_book_24)
                }
            } else {
                imageView.setImageResource(R.drawable.baseline_book_24)
            }

            buttonEdit.setOnClickListener {
                val intent = Intent(this, RecordActivity::class.java).apply {
                    putExtra("book_id", book.id)
                    putExtra("title", book.title)
                    putExtra("author", book.author)
                    putExtra("publisher", book.publisher)
                    putExtra("content", book.content)
                    putExtra("coverUri", book.coverUri)
                }
                startActivity(intent)
            }

            buttonDelete.setOnClickListener {
                val success = dbHelper.deleteBook(book.id)
                if (success) {
                    Toast.makeText(this, "삭제되었습니다", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }

            container.addView(itemView)
        }
    }
}
