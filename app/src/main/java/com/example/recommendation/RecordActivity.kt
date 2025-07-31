package com.example.recommendation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class RecordActivity : AppCompatActivity() {

    // 책 정보 입력을 위한 뷰 선언
    private lateinit var editTextTitle: EditText
    private lateinit var editTextAuthor: EditText
    private lateinit var editTextPublisher: EditText
    private lateinit var editTextContent: EditText
    private lateinit var imageViewCover: ImageView

    // 이미지 선택 요청 코드
    private val REQUEST_CODE_PICK_IMAGE = 1001
    private var selectedImageUri: Uri? = null

    // DB 헬퍼
    private lateinit var dbHelper: BookDatabaseHelper

    // 수정 중인 책의 ID (신규 등록 시 null)
    private var editingBookId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record)

        // DB 헬퍼 초기화
        dbHelper = BookDatabaseHelper(this)

        // View ID 연결
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextAuthor = findViewById(R.id.editTextAuthor)
        editTextPublisher = findViewById(R.id.editTextPublisher)
        editTextContent = findViewById(R.id.editTextContent)
        imageViewCover = findViewById(R.id.imageViewCover)

        // 버튼 초기화
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        val buttonHome = findViewById<Button>(R.id.buttonHome)

        // 인텐트로 전달된 book_id를 통해 수정 여부 확인
        editingBookId = intent.getIntExtra("book_id", -1).takeIf { it != -1 }

        // 수정 모드일 경우 기존 정보 채우기
        if (editingBookId != null) {
            editTextTitle.setText(intent.getStringExtra("title") ?: "")
            editTextAuthor.setText(intent.getStringExtra("author") ?: "")
            editTextPublisher.setText(intent.getStringExtra("publisher") ?: "")
            editTextContent.setText(intent.getStringExtra("content") ?: "")
            val coverUriString = intent.getStringExtra("coverUri")
            if (!coverUriString.isNullOrEmpty()) {
                selectedImageUri = Uri.parse(coverUriString)
                Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.baseline_book_24)
                    .error(R.drawable.baseline_book_24)
                    .into(imageViewCover)
            }
        }

        // 이미지 선택 버튼 클릭
        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        // 저장 버튼 클릭
        buttonSubmit.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val author = editTextAuthor.text.toString().trim()
            val publisher = editTextPublisher.text.toString().trim()
            val content = editTextContent.text.toString().trim()
            val coverUriString = selectedImageUri?.toString()

            // 필수 입력 검증
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

            // DB 저장 (수정 또는 신규 등록)
            val success = if (editingBookId != null) {
                dbHelper.updateBook(
                    id = editingBookId!!,
                    title = title,
                    author = author,
                    publisher = publisher.takeIf { it.isNotEmpty() },
                    content = content.takeIf { it.isNotEmpty() },
                    coverUri = coverUriString
                )
            } else {
                dbHelper.insertBook(
                    title = title,
                    author = author,
                    publisher = publisher.takeIf { it.isNotEmpty() },
                    content = content.takeIf { it.isNotEmpty() },
                    coverUri = coverUriString
                )
            }

            // 결과 처리
            if (success) {
                Toast.makeText(this, "저장되었습니다", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, BookData::class.java))
                finish()
            } else {
                Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }

        // 홈 버튼 클릭 시 메인 화면으로 이동
        buttonHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
        }
    }

    // 이미지 선택 후 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            if (selectedImageUri != null) {
                try {
                    // 영구 접근 권한 부여
                    contentResolver.takePersistableUriPermission(
                        selectedImageUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
                // 이미지 뷰에 선택한 이미지 표시
                imageViewCover.setImageURI(selectedImageUri)
            }
        }
    }
}
