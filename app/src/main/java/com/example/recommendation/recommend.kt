package com.example.recommendation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class RecommendActivity : AppCompatActivity() {

    // Google Gemini API 키
    private val apiKey = "AIzaSyCDpGTAPOkUxzP5EQaVVUNf2J5TXQijXHA"

    // 최근 책 제목과 인기 책 제목 TextView 리스트
    private lateinit var recentBookTitles: List<TextView>
    private lateinit var popularBookTitles: List<TextView>

    // 각각의 책 이미지 ImageView 리스트
    private lateinit var recentImageViews: List<ImageView>
    private lateinit var popularImageViews: List<ImageView>

    // 로컬 SQLite 데이터베이스 헬퍼
    private lateinit var dbHelper: BookDatabaseHelper

    // HTTP 클라이언트 설정 (타임아웃 등)
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recommend)

        // DB 헬퍼 초기화
        dbHelper = BookDatabaseHelper(this)

        // TextView 및 ImageView 연결
        recentBookTitles = listOf(
            findViewById(R.id.recentBookTitle1),
            findViewById(R.id.recentBookTitle2),
            findViewById(R.id.recentBookTitle3)
        )
        popularBookTitles = listOf(
            findViewById(R.id.popularBookTitle1),
            findViewById(R.id.popularBookTitle2),
            findViewById(R.id.popularBookTitle3)
        )

        recentImageViews = listOf(
            findViewById(R.id.imageView9),
            findViewById(R.id.imageView21),
            findViewById(R.id.imageView11)
        )
        popularImageViews = listOf(
            findViewById(R.id.imageView15),
            findViewById(R.id.imageView17),
            findViewById(R.id.imageView14)
        )

        // 홈 버튼 클릭 시 메인 액티비티로 이동
        findViewById<Button>(R.id.buttonHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // 최근 읽은 책 기반 추천 요청
        CoroutineScope(Dispatchers.Main).launch {
            val recentReadBooks = getRecentBookTitles(3)
            val recentPrompt = if (recentReadBooks.isNotEmpty()) {
                "User recently read: ${recentReadBooks.joinToString(", ")}. Recommend 3 similar genre books. Just give titles separated by commas. No numbers or explanations."
            } else {
                "Recommend 3 books of a popular genre. Just give titles separated by commas. No numbers or explanations."
            }

            // AI에게 추천 요청
            val recommendations = fetchRecommendation(recentPrompt)
            val bookTitles = parseBookTitles(recommendations)

            // 추천 결과를 TextView에 표시
            bookTitles.forEachIndexed { index, title ->
                if (index < recentBookTitles.size) {
                    recentBookTitles[index].text = title
                }
            }
        }

        // 인기 책 추천 요청
        CoroutineScope(Dispatchers.Main).launch {
            val popularPrompt = "Recommend 3 current popular bestselling books. Just titles, no numbers or explanations. No need for real-time accuracy."
            val recommendations = fetchRecommendation(popularPrompt)
            val bookTitles = parseBookTitles(recommendations)

            // 추천 결과를 TextView에 표시
            bookTitles.forEachIndexed { index, title ->
                if (index < popularBookTitles.size) {
                    popularBookTitles[index].text = title
                }
            }
        }

        // 이미지 클릭 이벤트 설정
        setImageViewClickEvents()
    }

    // 이미지 클릭 시 팝업 보여주기 위한 이벤트 등록
    private fun setImageViewClickEvents() {
        for (i in recentImageViews.indices) {
            recentImageViews[i].setOnClickListener {
                val title = recentBookTitles[i].text.toString()
                showBookPopup(title)
            }
        }

        for (i in popularImageViews.indices) {
            popularImageViews[i].setOnClickListener {
                val title = popularBookTitles[i].text.toString()
                showBookPopup(title)
            }
        }
    }

    // 책 제목을 클릭했을 때 팝업으로 책 요약 및 정보 보여주기
    private fun showBookPopup(title: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val book = dbHelper.getAllBooks().find { it.title == title }
            val prompt = "Book title: \"$title\". Summarize it in one short sentence."

            // AI로부터 요약 요청
            val aiDescription = fetchRecommendation(prompt)

            // 팝업 메시지 구성
            val message = buildString {
                if (book != null) {
                    append("Author: ${book.author}\n\n")
                }
                append(aiDescription)
            }

            // AlertDialog로 책 정보 표시
            AlertDialog.Builder(this@RecommendActivity)
                .setTitle(book?.title ?: title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    // Google Gemini API로 AI 응답 받기
    private suspend fun fetchRecommendation(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val payload = mapOf(
                "contents" to listOf(
                    mapOf("parts" to listOf(mapOf("text" to prompt)))
                )
            )

            val requestBody = Gson().toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "API request failed: ${response.code}"
                val body = response.body?.string() ?: return@withContext "No response body"
                val result = Gson().fromJson(body, GeminiResponse::class.java)
                return@withContext result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No recommendation returned"
            }
        } catch (e: Exception) {
            return@withContext "Error occurred: ${e.message ?: "Unknown error"}"
        }
    }

    // AI 응답으로부터 책 제목 리스트 추출
    private fun parseBookTitles(text: String): List<String> {
        return text.split("\n", ",") // 줄바꿈 또는 쉼표 기준 분리
            .map { it.trim() } // 공백 제거
            .filter { it.isNotEmpty() } // 빈 항목 제거
            .map { it.replace(Regex("^\\d+\\.?\\s*"), "") } // 번호 형식 제거 (예: "1. ")
    }

    // 최근 읽은 책 제목을 DB에서 최대 N개까지 가져오기
    private fun getRecentBookTitles(limit: Int): List<String> {
        return dbHelper.getAllBooks().take(limit).map { it.title }
    }

    // Gemini API 응답 객체 구조
    data class GeminiResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
}
