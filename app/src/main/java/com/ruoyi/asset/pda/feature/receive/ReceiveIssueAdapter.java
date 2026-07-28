package com.ruoyi.asset.pda.feature.receive;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemReceiveIssueBinding;

import java.util.ArrayList;
import java.util.List;

/** 将 UNKNOWN、INELIGIBLE、DUPLICATE 保持为可滚动的逐行异常清单。 */
public final class ReceiveIssueAdapter
        extends RecyclerView.Adapter<ReceiveIssueAdapter.Holder> {
    private final List<ReceiveIssueItem> values = new ArrayList<>();

    public void submit(List<ReceiveIssueItem> items) {
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
        return new Holder(ItemReceiveIssueBinding.inflate(
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
        private final ItemReceiveIssueBinding binding;

        private Holder(ItemReceiveIssueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(ReceiveIssueItem item) {
            binding.receiveIssueIdentifier.setText(value(item.getIdentifyValue()));
            binding.receiveIssueAsset.setText(asset(item));
            binding.receiveIssueReason.setText(value(item.getMessage()));
        }

        private String asset(ReceiveIssueItem item) {
            if (hasText(item.getAssetCode()) || hasText(item.getAssetName())) {
                return value(item.getAssetCode()) + " · " + value(item.getAssetName());
            }
            return value(item.getStatus());
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
