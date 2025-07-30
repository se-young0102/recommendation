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

class RecommendActivity : AppCompatActivity() {

    private val apiKey = "AIzaSyCDpGTAPOkUxzP5EQaVVUNf2J5TXQijXHA"

    private lateinit var recentBookTitles: List<TextView>
    private lateinit var popularBookTitles: List<TextView>

    private lateinit var recentImageViews: List<ImageView>
    private lateinit var popularImageViews: List<ImageView>

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

        findViewById<Button>(R.id.buttonHome).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

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

        CoroutineScope(Dispatchers.Main).launch {
            val popularPrompt = "요즘 인기 있는 베스트셀러 책 3권을 알아서 추천해줘. 제목만 알려줘. 번호나 부가 설명 없이 책 제목만 쉼표로 구분해서 알려줘."
            val recommendations = fetchRecommendation(popularPrompt)
            val bookTitles = parseBookTitles(recommendations)

            bookTitles.forEachIndexed { index, title ->
                if (index < popularBookTitles.size) {
                    popularBookTitles[index].text = title
                }
            }
        }

        setImageViewClickEvents()
    }

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

    private fun showBookPopup(title: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val book = dbHelper.getAllBooks().find { it.title == title }

            // AI 요약 요청
            val prompt = "책 제목: \"$title\". 이 책을 한 줄로 소개해줘. 너무 길게 말하지 말고 간단히 알려줘."
            val aiDescription = fetchRecommendation(prompt)

            val message = buildString {
                if (book != null) {
                    append("저자: ${book.author}\n\n")
                }
                append("$aiDescription")
            }

            AlertDialog.Builder(this@RecommendActivity)
                .setTitle(book?.title ?: title)
                .setMessage(message)
                .setPositiveButton("닫기", null)
                .show()
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
        val books = dbHelper.getAllBooks()
        return books.take(limit).map { it.title }
    }

    data class GeminiResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
}
