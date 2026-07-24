package com.ruoyi.asset.pda.feature.inbound;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemInboundAssetBinding;

import java.util.ArrayList;
import java.util.List;

public final class InboundAssetAdapter
        extends RecyclerView.Adapter<InboundAssetAdapter.Holder> {
    public interface Listener {
        void onOpen(InboundAssetItem item);

        void onRemove(InboundAssetItem item);
    }

    private final Listener listener;
    private final List<InboundAssetItem> values = new ArrayList<>();

    public InboundAssetAdapter(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("入库资产监听器不能为空");
        }
        this.listener = listener;
    }

    public void submit(List<InboundAssetItem> items) {
        if (sameItems(items)) {
            return;
        }
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

    private boolean sameItems(List<InboundAssetItem> items) {
        int nextSize = items == null ? 0 : items.size();
        if (values.size() != nextSize) {
            return false;
        }
        for (int index = 0; index < nextSize; index++) {
            InboundAssetItem current = values.get(index);
            InboundAssetItem next = items.get(index);
            if (current == null || next == null
                    || !current.getAssetId().equals(next.getAssetId())) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemInboundAssetBinding.inflate(
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

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemInboundAssetBinding binding;

        private Holder(ItemInboundAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(InboundAssetItem item) {
            binding.inboundAssetTitle.setText(join(item.getAssetCode(),
                    item.getAssetName()));
            binding.inboundAssetMeta.setText(join(item.getCategoryName(),
                    item.getAssetStatusLabel()));
            binding.inboundAssetSource.setText(item.getSource()
                    == InboundAssetItem.Source.RFID
                    ? R.string.inbound_source_rfid
                    : R.string.inbound_source_asset_code);
            binding.getRoot().setOnClickListener(view -> listener.onOpen(item));
            binding.inboundAssetRemove.setOnClickListener(
                    view -> listener.onRemove(item));
        }

        private String join(String first, String second) {
            String left = value(first);
            String right = value(second);
            return left + " · " + right;
        }

        private String value(String value) {
            return value == null || value.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.common_unknown)
                    : value.trim();
        }
    }
}
