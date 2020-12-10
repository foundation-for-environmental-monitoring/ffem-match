package io.ffem.lite.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import io.ffem.lite.R
import kotlinx.android.synthetic.main.fragment_result.*


class SaveResult: AppCompatActivity(){

    lateinit var name: TextView
    lateinit var value: TextView
    lateinit var result: TextView
    lateinit var unit: TextView
    private lateinit var submit: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_result)

        name = findViewById(R.id.name_txt)
        value = findViewById(R.id.value_txt)
        result = findViewById(R.id.result_txt)
        unit = findViewById(R.id.unit_txt)
        submit = findViewById(R.id.submit_btn)


        submit.setOnClickListener{
            saveHistory()
        }
    }

    private fun saveHistory(){
        val name = name_txt.text.toString().trim()
        val value = value_txt.text.toString().trim()
        val result = result_txt.text.toString().trim()
        val unit = unit_txt.text.toString().trim()

        if (name.isEmpty() && value.isEmpty() && result.isEmpty() && unit.isEmpty() ){
            name_txt.error = "Test Details not saved"
            return
        }

        val ref = FirebaseDatabase.getInstance().getReference("history")
        val historyId = ref.push().key
        historyId?.let { History(it, name, value, result, unit) }
    }
}