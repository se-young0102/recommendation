package com.example.recommendation

import android.content.Intent
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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

class RecommendActivity : AppCompatActivity() {

    private val apiKey = "AIzaSyC884WdzEM-hJrn7xoWMTPxKQ6v4bdVoAw"

    private lateinit var recentBookTitles: List<TextView>
    private lateinit var popularBookTitles: List<TextView>

    private lateinit var dbHelper: BookDatabaseHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recommend)

        dbHelper = BookDatabaseHelper(this)

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

        findViewById<Button>(R.id.buttonHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // 최근 읽은 책과 비슷한 책 추천 요청
        CoroutineScope(Dispatchers.Main).launch {
            val recentReadBooks = getRecentBookTitles(3)
            val recentPrompt = if (recentReadBooks.isNotEmpty()) {
                "사용자가 최근에 읽은 책들: ${recentReadBooks.joinToString(", ")}. 이와 비슷한 장르의 책 3권을 추천해줘. 제목만 알려줘. 번호나 부가 설명 없이 책 제목만 쉼표로 구분해서 알려줘."
            } else {
                "사용자가 최근에 읽은 책과 비슷한 장르의 책 3권을 추천해줘. 제목만 알려줘. 번호나 부가 설명 없이 책 제목만 쉼표로 구분해서 알려줘."
            }

            val recommendations = fetchRecommendation(recentPrompt)
            val bookTitles = parseBookTitles(recommendations)

            bookTitles.forEachIndexed { index, title ->
                if (index < recentBookTitles.size) {
                    recentBookTitles[index].text = title
                }
            }
        }

        // 인기 책 추천 요청
        CoroutineScope(Dispatchers.Main).launch {
            val popularPrompt = "현재 대한민국에서 가장 인기 있는 베스트셀러 책 3권을 추천해줘. 제목만 알려줘. 번호나 부가 설명 없이 책 제목만 쉼표로 구분해서 알려줘."
            val recommendations = fetchRecommendation(popularPrompt)
            val bookTitles = parseBookTitles(recommendations)

            bookTitles.forEachIndexed { index, title ->
                if (index < popularBookTitles.size) {
                    popularBookTitles[index].text = title
                }
            }
        }
    }

    private suspend fun fetchRecommendation(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

            val payload = mapOf(
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to prompt)
                        )
                    )
                )
            )

            val requestBody = Gson().toJson(payload).toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "API 호출 실패: ${response.code}"
                val body = response.body?.string() ?: return@withContext "응답 없음"
                val result = Gson().fromJson(body, GeminiResponse::class.java)
                return@withContext result.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "추천 결과 없음"
            }
        } catch (e: Exception) {
            return@withContext "오류 발생: ${e.message}"
        }
    }

    private fun parseBookTitles(text: String): List<String> {
        return text.split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("^\\d+\\.?\\s*"), "") }
    }

    private fun getRecentBookTitles(limit: Int): List<String> {
        val books = dbHelper.getAllBooks()  // 최신순 정렬로 받아옴
        return books.take(limit).map { it.title }
    }

    data class GeminiResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
}
