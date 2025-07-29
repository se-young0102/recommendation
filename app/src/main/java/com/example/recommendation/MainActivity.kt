package com.example.recommendation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: BookDatabaseHelper
    private lateinit var adapter: SearchResultAdapter
    private lateinit var searchResultRecyclerView: RecyclerView

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 권한 체크 및 요청
        checkAndRequestPermissions()

        dbHelper = BookDatabaseHelper(this)

        val searchView = findViewById<SearchView>(R.id.searchView)
        searchResultRecyclerView = findViewById(R.id.searchResultRecyclerView)
        searchResultRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SearchResultAdapter(emptyList()) { bookItem ->
            // 검색 결과 클릭 시 원하는 동작 (예: 상세 페이지 이동 등)
            // Toast.makeText(this, "클릭: ${bookItem.title}", Toast.LENGTH_SHORT).show()
        }
        searchResultRecyclerView.adapter = adapter

        // 기록하기 버튼
        findViewById<Button>(R.id.button).setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        // 폴더 이미지 클릭 시 BookData 액티비티 실행
        findViewById<ImageView>(R.id.folder1).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }
        findViewById<ImageView>(R.id.folder2).setOnClickListener {
            startActivity(Intent(this, BookData::class.java))
        }

        // 도서추천 버튼 (기능 추후 추가 예정)
        findViewById<Button>(R.id.button3).setOnClickListener {
            Toast.makeText(this, "도서추천 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }

        // 내가 읽은 도서 영역에 DB에서 최신 3개 책 제목 넣기
        updateReadBooksTitles()

        // SearchView 텍스트 변경 리스너
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // 검색 버튼 눌렀을 때 처리 (필요 시 구현)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    adapter.updateData(emptyList())
                    searchResultRecyclerView.visibility = RecyclerView.GONE
                } else {
                    val results = dbHelper.searchBooksByTitle(newText)
                    adapter.updateData(results)
                    searchResultRecyclerView.visibility =
                        if (results.isEmpty()) RecyclerView.GONE else RecyclerView.VISIBLE
                }
                return true
            }
        })
    }

    private fun updateReadBooksTitles() {
        val bookList = dbHelper.getAllBooks().sortedByDescending { it.id }
        val titles = bookList.take(3).map { it.title }

        findViewById<TextView>(R.id.bookAText).text = titles.getOrNull(0) ?: "도서 없음"
        findViewById<TextView>(R.id.bookBText).text = titles.getOrNull(1) ?: ""
        findViewById<TextView>(R.id.bookCText).text = titles.getOrNull(2) ?: ""
    }

    private fun checkAndRequestPermissions() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "권한이 거부되어 앱을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

// 검색 결과 RecyclerView 어댑터
class SearchResultAdapter(
    private var items: List<BookItem>,
    private val onItemClick: (BookItem) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(android.R.id.text1)

        init {
            itemView.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.titleText.text = items[position].title
    }

    fun updateData(newItems: List<BookItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
