package io.ffem.lite.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.app.AppDatabase
import io.ffem.lite.model.TestResult

class ResultListActivity : AppCompatActivity() {

    private lateinit var listView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_list)

        val db = AppDatabase.getDatabase(this)

        db.resultDao().insert(TestResult(1, "testing"))
        val resultList = db.resultDao().getResults()
        val listItems = arrayOfNulls<String>(resultList.size)
        for (i in 0 until resultList.size) {
            val result = resultList[i]
            listItems[i] = result.name
        }
        val adapter = ResultAdapter()

        adapter.setTestList(resultList)

        listView = findViewById(R.id.list_results)

        listView.adapter = adapter

    }
}
