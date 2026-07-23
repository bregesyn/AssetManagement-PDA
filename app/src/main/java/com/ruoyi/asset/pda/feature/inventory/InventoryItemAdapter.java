package com.ruoyi.asset.pda.feature.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.databinding.ItemInventoryItemBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class InventoryItemAdapter
        extends RecyclerView.Adapter<InventoryItemAdapter.ItemViewHolder> {
    public interface Listener {
        void onCorrect(PdaInventoryItemDto item);

        void onPreviewSelectionChanged(String epcCode, boolean selected);
    }

    private final Listener listener;
    private final List<PdaInventoryItemDto> items = new ArrayList<>();
    private Map<Long, String> resultOverrides = Collections.emptyMap();
    private Map<Long, String> previewEpcByItemId = Collections.emptyMap();
    private Set<String> selectedEpcs = Collections.emptySet();
    private boolean canEdit;

    public InventoryItemAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PdaInventoryItemDto> values, Map<Long, String> resultOverrides,
            Map<Long, String> previewEpcByItemId, Set<String> selectedEpcs, boolean canEdit) {
        items.clear();
        if (values != null) {
            items.addAll(values);
        }
        this.resultOverrides = resultOverrides == null ? Collections.emptyMap() : resultOverrides;
        this.previewEpcByItemId = previewEpcByItemId == null
                ? Collections.emptyMap() : previewEpcByItemId;
        this.selectedEpcs = selectedEpcs == null ? Collections.emptySet() : selectedEpcs;
        this.canEdit = canEdit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(ItemInventoryItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        PdaInventoryItemDto item = items.get(position);
        holder.binding.inventoryItemAsset.setText(format(item.getAssetCode(), item.getAssetName()));
        holder.binding.inventoryItemBook.setText(formatLocation(
                item.getBookWarehouseName(), item.getBookLocationName()));
        Long itemId = item.getItemId();
        String result = itemId != null && resultOverrides.containsKey(itemId)
                ? resultOverrides.get(itemId) : item.getInventoryResult();
        String previewEpc = itemId == null ? null : previewEpcByItemId.get(itemId);
        boolean awaitingConfirmation = !hasText(result) && hasText(previewEpc);
        boolean selected = awaitingConfirmation && selectedEpcs.contains(previewEpc);

        if (awaitingConfirmation) {
            holder.binding.inventoryItemResult.setText(selected
                    ? "本轮已扫 · 待确认" : "本轮已扫 · 未选中");
            holder.binding.inventoryItemStatusDot.setBackgroundResource(R.drawable.inventory_status_pending_dot);
            holder.binding.inventoryItemResult.setTextColor(holder.itemView.getContext().getColor(
                    R.color.pda_primary));
        } else if ("NORMAL".equals(result)) {
            holder.binding.inventoryItemResult.setText("正常");
            holder.binding.inventoryItemStatusDot.setBackgroundResource(R.drawable.inventory_status_normal_dot);
            holder.binding.inventoryItemResult.setTextColor(holder.itemView.getContext().getColor(
                    R.color.pda_success));
        } else if ("LOSS".equals(result)) {
            holder.binding.inventoryItemResult.setText("盘亏");
            holder.binding.inventoryItemStatusDot.setBackgroundResource(R.drawable.inventory_status_loss_dot);
            holder.binding.inventoryItemResult.setTextColor(holder.itemView.getContext().getColor(
                    R.color.pda_error));
        } else {
            holder.binding.inventoryItemResult.setText("未盘");
            holder.binding.inventoryItemStatusDot.setBackgroundResource(R.drawable.inventory_status_uninventoried_dot);
            holder.binding.inventoryItemResult.setTextColor(holder.itemView.getContext().getColor(
                    R.color.pda_text_secondary));
        }
        holder.binding.getRoot().setEnabled(canEdit);
        holder.binding.getRoot().setOnClickListener(view -> {
            if (!canEdit || listener == null) {
                return;
            }
            if (awaitingConfirmation) {
                listener.onPreviewSelectionChanged(previewEpc, !selected);
            } else {
                listener.onCorrect(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String format(String code, String name) {
        if (hasText(code) && hasText(name)) {
            return code + " · " + name;
        }
        return hasText(code) ? code : (hasText(name) ? name : "未命名资产");
    }

    private String formatLocation(String warehouse, String location) {
        if (hasText(warehouse) && hasText(location)) {
            return "账面 " + warehouse + " · " + location;
        }
        return hasText(warehouse) ? "账面 " + warehouse : "账面位置未知";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static final class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventoryItemBinding binding;

        ItemViewHolder(ItemInventoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
