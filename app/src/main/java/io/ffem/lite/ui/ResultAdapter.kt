package io.ffem.lite.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import io.ffem.lite.R
import io.ffem.lite.databinding.RowResultBinding
import io.ffem.lite.model.TestResult

class ResultAdapter : RecyclerView.Adapter<ResultAdapter.TestResultViewHolder>() {

    private var testList: List<TestResult>? = null

    internal fun setTestList(testList: List<TestResult>) {
        this.testList = testList
        notifyItemRangeInserted(0, testList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestResultViewHolder {
        val binding = DataBindingUtil
            .inflate<RowResultBinding>(
                LayoutInflater.from(parent.context), R.layout.row_result,
                parent, false
            )
        return TestResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestResultViewHolder, position: Int) {
        holder.binding.result = testList!![position]
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return if (testList == null) 0 else testList!!.size
    }

    internal fun getItemAt(i: Int): TestResult {
        return testList!![i]
    }

    class TestResultViewHolder(val binding: RowResultBinding) : RecyclerView.ViewHolder(binding.root)
}
