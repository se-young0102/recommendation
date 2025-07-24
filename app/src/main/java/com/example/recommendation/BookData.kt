package com.example.recommendation

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class BookData : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.check) // check.xml 연결

        val imageView = findViewById<ImageView>(R.id.imageViewCheckCover)

        // SharedPreferences에서 URI 불러오기
        val prefs = getSharedPreferences("book_pref", MODE_PRIVATE)
        val uriString = prefs.getString("book_cover_uri", null)

        if (uriString != null) {
            val imageUri = Uri.parse(uriString)

        }
    }
}