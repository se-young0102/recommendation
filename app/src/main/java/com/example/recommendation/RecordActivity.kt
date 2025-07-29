package com.example.recommendation

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class RecordActivity : AppCompatActivity() {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextAuthor: EditText
    private lateinit var editTextPublisher: EditText
    private lateinit var editTextContent: EditText
    private lateinit var imageViewCover: ImageView

    private val REQUEST_CODE_PICK_IMAGE = 1001
    private var savedImagePath: String? = null

    private lateinit var dbHelper: BookDatabaseHelper

    private var editingBookId: Int? = null

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

        editingBookId = intent.getIntExtra("book_id", -1).takeIf { it != -1 }
        if (editingBookId != null) {
            editTextTitle.setText(intent.getStringExtra("title") ?: "")
            editTextAuthor.setText(intent.getStringExtra("author") ?: "")
            editTextPublisher.setText(intent.getStringExtra("publisher") ?: "")
            editTextContent.setText(intent.getStringExtra("content") ?: "")

            val path = intent.getStringExtra("coverPath")
            if (!path.isNullOrEmpty()) {
                savedImagePath = path
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) imageViewCover.setImageBitmap(bitmap)
                else imageViewCover.setImageResource(R.drawable.baseline_book_24)
            }
        }

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        buttonSubmit.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val author = editTextAuthor.text.toString().trim()
            val publisher = editTextPublisher.text.toString().trim()
            val content = editTextContent.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "책 제목을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (author.isEmpty()) {
                Toast.makeText(this, "저자를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (savedImagePath == null) {
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
                    coverUri = savedImagePath
                )
            } else {
                dbHelper.insertBook(
                    title = title,
                    author = author,
                    publisher = if (publisher.isEmpty()) null else publisher,
                    content = if (content.isEmpty()) null else content,
                    coverUri = savedImagePath
                )
            }

            if (success) {
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val path = copyImageToInternalStorage(uri)
                if (path != null) {
                    savedImagePath = path
                    val bitmap = BitmapFactory.decodeFile(path)
                    imageViewCover.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        val filename = "cover_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, filename)
        return try {
            contentResolver.openInputStream(uri).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
