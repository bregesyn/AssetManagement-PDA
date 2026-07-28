package com.ruoyi.asset.pda.feature.receive;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemReceiveAssetBinding;

import java.util.ArrayList;
import java.util.List;

/** 已预检资产以紧凑行呈现，点击查看事实、移除误扫项。 */
public final class ReceiveAssetAdapter
        extends RecyclerView.Adapter<ReceiveAssetAdapter.Holder> {
    public interface Listener {
        void onOpen(ReceiveAssetItem item);

        void onRemove(ReceiveAssetItem item);
    }

    private final Listener listener;
    private final List<ReceiveAssetItem> values = new ArrayList<>();

    public ReceiveAssetAdapter(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("领用资产监听器不能为空");
        }
        this.listener = listener;
    }

    public void submit(List<ReceiveAssetItem> items) {
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

    private boolean sameItems(List<ReceiveAssetItem> items) {
        int nextSize = items == null ? 0 : items.size();
        if (values.size() != nextSize) {
            return false;
        }
        for (int index = 0; index < nextSize; index++) {
            ReceiveAssetItem current = values.get(index);
            ReceiveAssetItem next = items.get(index);
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
        return new Holder(ItemReceiveAssetBinding.inflate(
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
        private final ItemReceiveAssetBinding binding;

        private Holder(ItemReceiveAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(ReceiveAssetItem item) {
            binding.receiveAssetTitle.setText(join(item.getAssetCode(),
                    item.getAssetName()));
            binding.receiveAssetMeta.setText(join(item.getCategoryName(),
                    item.getAssetStatusLabel()));
            binding.receiveAssetSource.setText(item.getSource()
                    == ReceiveAssetItem.Source.RFID
                    ? R.string.receive_source_rfid
                    : R.string.receive_source_asset_code);
            binding.getRoot().setOnClickListener(view -> listener.onOpen(item));
            binding.receiveAssetRemove.setOnClickListener(
                    view -> listener.onRemove(item));
        }

        private String join(String first, String second) {
            return value(first) + " · " + value(second);
        }

        private String value(String value) {
            return value == null || value.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.common_unknown)
                    : value.trim();
        }
    }
}
