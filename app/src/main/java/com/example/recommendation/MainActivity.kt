package com.example.recommendation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // activity_main.xml에 연결

        // 기록하기 버튼 -> RecordActivity 이동
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            val intent = Intent(this, RecordActivity::class.java)
            startActivity(intent)
        }

        // 폴더1 클릭 시 -> BookData 액티비티로 이동
        val folder1 = findViewById<ImageView>(R.id.folder1)
        folder1.setOnClickListener {
            val intent = Intent(this, BookData::class.java)
            startActivity(intent)
        }

        // 폴더2 클릭 시 -> BookData 액티비티로 이동
        val folder2 = findViewById<ImageView>(R.id.folder2)
        folder2.setOnClickListener {
            val intent = Intent(this, BookData::class.java)
            startActivity(intent)
        }

        // 도서추천 버튼 클릭 시 다른 기능 넣고 싶다면 여기에 추가하면 됩니다
        val buttonRecommend = findViewById<Button>(R.id.button3)
        buttonRecommend.setOnClickListener {
            // 예시: Toast 메시지
            // Toast.makeText(this, "도서추천 기능 준비중!", Toast.LENGTH_SHORT).show()
        }

        // 홈 버튼은 현재 MainActivity에 있기 때문에 동작 불필요
        // 필요하면 새로 정의해도 됩니다
    }
}
