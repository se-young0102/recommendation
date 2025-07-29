package com.example.recommendation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RecordActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextAuthor: EditText
    private lateinit var editTextPublisher: EditText
    private lateinit var editTextContent: EditText
    private lateinit var imageViewCover: ImageView

    private val REQUEST_CODE_PICK_IMAGE = 1001
    private var selectedImageUri: Uri? = null

    private lateinit var dbHelper: BookDatabaseHelper

    private var editingBookId: Int? = null // 수정 모드 식별자

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record)

        dbHelper = BookDatabaseHelper(this)

        editTextTitle = findViewById(R.id.editTextTitle)
        editTextAuthor = findViewById(R.id.editTextAuthor)
        editTextPublisher = findViewById(R.id.editTextPublisher)
        editTextContent = findViewById(R.id.editTextContent)
        imageViewCover = findViewById(R.id.imageViewCover)

        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        val buttonHome = findViewById<Button>(R.id.buttonHome)

        // Intent로 넘어온 데이터 받기 (수정 모드)
        editingBookId = intent.getIntExtra("book_id", -1).takeIf { it != -1 }
        if (editingBookId != null) {
            editTextTitle.setText(intent.getStringExtra("title"))
            editTextAuthor.setText(intent.getStringExtra("author"))
            editTextPublisher.setText(intent.getStringExtra("publisher"))
            editTextContent.setText(intent.getStringExtra("content"))

            val coverUriString = intent.getStringExtra("coverUri")
            if (!coverUriString.isNullOrEmpty()) {
                selectedImageUri = Uri.parse(coverUriString)
                imageViewCover.setImageURI(selectedImageUri)
            }
        }

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        buttonSubmit.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val author = editTextAuthor.text.toString().trim()
            val publisher = editTextPublisher.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val coverUriString = selectedImageUri?.toString()

            if (title.isEmpty()) {
                Toast.makeText(this, "책 제목을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (author.isEmpty()) {
                Toast.makeText(this, "저자를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedImageUri == null) {
                Toast.makeText(this, "책 표지 이미지를 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = if (editingBookId != null) {
                dbHelper.updateBook(
                    id = editingBookId!!,
                    title = title,
                    author = author,
                    publisher = if (publisher.isEmpty()) null else publisher,
                    content = if (content.isEmpty()) null else content,
                    coverUri = coverUriString
                )
            } else {
                dbHelper.insertBook(
                    title = title,
                    author = author,
                    publisher = if (publisher.isEmpty()) null else publisher,
                    content = if (content.isEmpty()) null else content,
                    coverUri = coverUriString
                )
            }

            if (success) {
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, BookData::class.java))
                finish()
            } else {
                Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }

        buttonHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                imageViewCover.setImageURI(selectedImageUri)
            }
        }
    }
}
