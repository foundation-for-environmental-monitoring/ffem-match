package io.ffem.lite.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import io.ffem.lite.R;
import io.ffem.lite.databinding.RowResultBinding;
import io.ffem.lite.model.TestResult;

import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.TestResultViewHolder> {

    @Nullable
    private List<? extends TestResult> mTestList;

    void setTestList(final List<? extends TestResult> testList) {
        if (mTestList != null) {
            mTestList.clear();
        }
        mTestList = testList;
        notifyItemRangeInserted(0, testList.size());
    }

    @NonNull
    @Override
    public TestResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowResultBinding binding = DataBindingUtil
                .inflate(LayoutInflater.from(parent.getContext()), R.layout.row_result,
                        parent, false);
        return new TestResultViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TestResultViewHolder holder, int position) {
        holder.binding.setResult(mTestList.get(position));
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mTestList == null ? 0 : mTestList.size();
    }

    TestResult getItemAt(int i) {
        return mTestList.get(i);
    }

    static class TestResultViewHolder extends RecyclerView.ViewHolder {

        final RowResultBinding binding;

        TestResultViewHolder(RowResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
