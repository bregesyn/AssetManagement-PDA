package com.ruoyi.asset.pda.feature.borrow;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemBorrowIssueBinding;

import java.util.ArrayList;
import java.util.List;

/** 预检异常逐行展示，避免异常项从提交清单中静默消失。 */
public final class BorrowIssueAdapter extends RecyclerView.Adapter<BorrowIssueAdapter.Holder> {
    private final List<BorrowIssueItem> values = new ArrayList<>();

    public void submit(List<BorrowIssueItem> items) {
        int previousSize = values.size();
        values.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        if (items != null) {
            values.addAll(items);
        }
        if (!values.isEmpty()) {
            notifyItemRangeInserted(0, values.size());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBorrowIssueBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(values.get(position));
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemBorrowIssueBinding binding;

        private Holder(ItemBorrowIssueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(BorrowIssueItem item) {
            binding.borrowIssueIdentifier.setText(value(item.getIdentifyValue()));
            binding.borrowIssueAsset.setText(hasText(item.getAssetCode())
                    || hasText(item.getAssetName())
                    ? value(item.getAssetCode()) + " · " + value(item.getAssetName())
                    : value(item.getStatus()));
            binding.borrowIssueReason.setText(value(item.getMessage()));
        }

        private String value(String value) {
            return hasText(value) ? value.trim()
                    : itemView.getContext().getString(R.string.common_unknown);
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
