package com.ruoyi.asset.pda.feature.inbound;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemInboundIssueBinding;

import java.util.ArrayList;
import java.util.List;

public final class InboundIssueAdapter
        extends RecyclerView.Adapter<InboundIssueAdapter.Holder> {
    private final List<InboundIssueItem> values = new ArrayList<>();

    public void submit(List<InboundIssueItem> items) {
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
        return new Holder(ItemInboundIssueBinding.inflate(
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
        private final ItemInboundIssueBinding binding;

        private Holder(ItemInboundIssueBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(InboundIssueItem item) {
            binding.inboundIssueEpc.setText(value(item.getEpcCode()));
            binding.inboundIssueAsset.setText(asset(item));
            binding.inboundIssueReason.setText(value(item.getMessage()));
        }

        private String asset(InboundIssueItem item) {
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
