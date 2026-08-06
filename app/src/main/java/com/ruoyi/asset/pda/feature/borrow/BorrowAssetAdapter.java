package com.ruoyi.asset.pda.feature.borrow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.databinding.ItemBorrowAssetBinding;

import java.util.ArrayList;
import java.util.List;

/** 服务端预检通过的资产清单；归还行额外展示借用单和借用前仓位。 */
public final class BorrowAssetAdapter extends RecyclerView.Adapter<BorrowAssetAdapter.Holder> {
    public interface Listener {
        void onOpen(BorrowAssetItem item);

        void onRemove(BorrowAssetItem item);
    }

    private final Listener listener;
    private final List<BorrowAssetItem> values = new ArrayList<>();

    public BorrowAssetAdapter(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("借还资产监听器不能为空");
        }
        this.listener = listener;
    }

    public void submit(List<BorrowAssetItem> items) {
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
        return new Holder(ItemBorrowAssetBinding.inflate(
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
        private final ItemBorrowAssetBinding binding;

        private Holder(ItemBorrowAssetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(BorrowAssetItem item) {
            binding.borrowAssetTitle.setText(join(item.getAssetCode(), item.getAssetName()));
            binding.borrowAssetMeta.setText(join(item.getCategoryName(),
                    item.getAssetStatusLabel()));
            binding.borrowAssetSource.setText(item.getSource() == BorrowAssetItem.Source.RFID
                    ? R.string.borrow_source_rfid : R.string.borrow_source_asset_code);
            if (hasText(item.getBorrowNo())) {
                binding.borrowAssetContext.setVisibility(View.VISIBLE);
                binding.borrowAssetContext.setText(itemView.getContext().getString(
                        R.string.borrow_asset_context_format, value(item.getBorrowNo()),
                        value(item.getBeforeWarehouseName()),
                        value(item.getBeforeLocationName())));
            } else {
                binding.borrowAssetContext.setVisibility(View.GONE);
            }
            binding.borrowAssetStatus.setText(hasText(item.getReturnStatus())
                    ? itemView.getContext().getString(R.string.borrow_asset_return_status_format,
                    value(item.getReturnStatus())) : itemView.getContext().getString(
                    R.string.borrow_asset_ready));
            binding.getRoot().setOnClickListener(view -> listener.onOpen(item));
            binding.borrowAssetRemove.setOnClickListener(view -> listener.onRemove(item));
        }

        private String join(String first, String second) {
            return value(first) + " · " + value(second);
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
