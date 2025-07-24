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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record)

        imageViewCover = findViewById(R.id.imageViewCover)
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit) // 입력하기 버튼

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        // ✅ 입력하기 버튼 누르면 Toast 메시지 띄우기
        buttonSubmit.setOnClickListener {
            Toast.makeText(this, "입력되었습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                imageViewCover.setImageURI(selectedImageUri)
            }
        }
    }
}
