package com.ruoyi.asset.pda.feature.inventory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.databinding.ItemInventoryPreviewBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InventoryPreviewAdapter
        extends RecyclerView.Adapter<InventoryPreviewAdapter.PreviewViewHolder> {
    public interface Listener {
        void onSelectionChanged(String epcCode, boolean selected);
    }

    private final Listener listener;
    private final List<PdaInventoryScanDto> rows = new ArrayList<>();
    private final Set<String> selected = new HashSet<>();
    private boolean canSubmit;

    public InventoryPreviewAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PdaInventoryScanDto> values, Set<String> selectedEpcs,
            boolean canSubmit) {
        rows.clear();
        if (values != null) {
            rows.addAll(values);
        }
        selected.clear();
        if (selectedEpcs != null) {
            selected.addAll(selectedEpcs);
        }
        this.canSubmit = canSubmit;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PreviewViewHolder(ItemInventoryPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        PdaInventoryScanDto row = rows.get(position);
        String epc = row == null ? null : row.getEpcCode();
        String asset = row == null ? null : row.getAssetCode();
        if (!hasText(asset) && row != null && row.getItem() != null) {
            asset = row.getItem().getAssetCode();
        }
        String name = row == null ? null : row.getAssetName();
        if (!hasText(name) && row != null && row.getItem() != null) {
            name = row.getItem().getAssetName();
        }
        boolean knownOutOfScope = row != null && "KNOWN_OUT_OF_SCOPE".equals(row.getMatchType());
        boolean confirmable = row != null && Boolean.TRUE.equals(row.getConfirmable());
        holder.binding.inventoryPreviewTitle.setText(knownOutOfScope && hasText(asset) ? asset
                : (knownOutOfScope && hasText(name) ? name : "异常 EPC · " + valueOrDash(epc)));
        holder.binding.inventoryPreviewDetail.setText(formatDetail(row));
        if (knownOutOfScope) {
            holder.binding.inventoryPreviewResult.setText("确认后登记盘盈");
        } else if (!confirmable) {
            holder.binding.inventoryPreviewResult.setText("不可确认，请检查标签台账");
        } else {
            holder.binding.inventoryPreviewResult.setText("待人工处理");
        }
        holder.binding.inventoryPreviewCheckbox.setVisibility(confirmable ? View.VISIBLE : View.INVISIBLE);
        holder.binding.inventoryPreviewCheckbox.setEnabled(canSubmit && confirmable);
        holder.binding.inventoryPreviewCheckbox.setOnCheckedChangeListener(null);
        holder.binding.inventoryPreviewCheckbox.setChecked(epc != null && selected.contains(epc));
        holder.binding.inventoryPreviewCheckbox.setOnCheckedChangeListener((button, checked) -> {
            if (listener != null && epc != null) {
                listener.onSelectionChanged(epc, checked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private String formatDetail(PdaInventoryScanDto row) {
        if (row == null) {
            return "-";
        }
        StringBuilder builder = new StringBuilder("KNOWN_OUT_OF_SCOPE".equals(row.getMatchType())
                ? "范围外已建档资产" : "数据异常");
        if (hasText(row.getMessage())) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(row.getMessage());
        }
        if (hasText(row.getEpcCode())) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("EPC ").append(row.getEpcCode());
        }
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private String valueOrDash(String value) {
        return hasText(value) ? value : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static final class PreviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventoryPreviewBinding binding;

        PreviewViewHolder(ItemInventoryPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
