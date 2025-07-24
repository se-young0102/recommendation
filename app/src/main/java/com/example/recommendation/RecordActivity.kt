package com.example.recommendation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RecordActivity : AppCompatActivity() {

    private lateinit var imageViewCover: ImageView
    private val REQUEST_CODE_PICK_IMAGE = 1001
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record)

        imageViewCover = findViewById(R.id.imageViewCover)
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        buttonSubmit.setOnClickListener {
            if (selectedImageUri != null) {
                // SharedPreferences에 이미지 URI 저장
                val prefs = getSharedPreferences("book_pref", MODE_PRIVATE)
                prefs.edit().putString("book_cover_uri", selectedImageUri.toString()).apply()

                Toast.makeText(this, "입력되었습니다", Toast.LENGTH_SHORT).show()

                // BookData 화면으로 이동
                val intent = Intent(this, BookData::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "책 표지를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
            }
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