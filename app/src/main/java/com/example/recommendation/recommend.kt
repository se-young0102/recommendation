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

    private val apiKey = "AIzaSyCDpGTAPOkUxzP5EQaVVUNf2J5TXQijXHA"

    private lateinit var recentBookTitles: List<TextView>
    private lateinit var popularBookTitles: List<TextView>
    private lateinit var recentImageViews: List<ImageView>
    private lateinit var popularImageViews: List<ImageView>
    private lateinit var dbHelper: BookDatabaseHelper

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
                "User recently read: ${recentReadBooks.joinToString(", ")}. Recommend 3 similar genre books. Just give titles separated by commas. No numbers or explanations."
            } else {
                "Recommend 3 books of a popular genre. Just give titles separated by commas. No numbers or explanations."
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
            val popularPrompt = "Recommend 3 current popular bestselling books. Just titles, no numbers or explanations. No need for real-time accuracy."
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
            val prompt = "Book title: \"$title\". Summarize it in one short sentence."

            val aiDescription = fetchRecommendation(prompt)

            val message = buildString {
                if (book != null) {
                    append("Author: ${book.author}\n\n")
                }
                append(aiDescription)
            }

            AlertDialog.Builder(this@RecommendActivity)
                .setTitle(book?.title ?: title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show()
        }
    }

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

    private fun parseBookTitles(text: String): List<String> {
        return text.split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("^\\d+\\.?\\s*"), "") }
    }

    private fun getRecentBookTitles(limit: Int): List<String> {
        return dbHelper.getAllBooks().take(limit).map { it.title }
    }

    data class GeminiResponse(val candidates: List<Candidate>?)
    data class Candidate(val content: Content)
    data class Content(val parts: List<Part>)
    data class Part(val text: String)
}

