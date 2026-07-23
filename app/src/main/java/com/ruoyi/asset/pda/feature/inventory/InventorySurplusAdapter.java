package com.ruoyi.asset.pda.feature.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.databinding.ItemInventorySurplusBinding;

import java.util.ArrayList;
import java.util.List;

public final class InventorySurplusAdapter
        extends RecyclerView.Adapter<InventorySurplusAdapter.SurplusViewHolder> {
    public interface Listener {
        void onDelete(PdaInventorySurplusDto surplus);
    }

    private final Listener listener;
    private final List<PdaInventorySurplusDto> surpluses = new ArrayList<>();
    private boolean canDelete;

    public InventorySurplusAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PdaInventorySurplusDto> values, boolean canDelete) {
        surpluses.clear();
        if (values != null) {
            surpluses.addAll(values);
        }
        this.canDelete = canDelete;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SurplusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SurplusViewHolder(ItemInventorySurplusBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SurplusViewHolder holder, int position) {
        PdaInventorySurplusDto surplus = surpluses.get(position);
        String identity = hasText(surplus.getAssetCode()) ? surplus.getAssetCode()
                : (hasText(surplus.getEpcCode()) ? surplus.getEpcCode() : "未命名盘盈");
        holder.binding.inventorySurplusIdentity.setText(identity);
        holder.binding.inventorySurplusDescription.setText(formatDescription(surplus));
        holder.binding.inventorySurplusDelete.setEnabled(canDelete);
        holder.binding.inventorySurplusDelete.setOnClickListener(view -> {
            if (listener != null) {
                listener.onDelete(surplus);
            }
        });
    }

    @Override
    public int getItemCount() {
        return surpluses.size();
    }

    private String formatDescription(PdaInventorySurplusDto surplus) {
        String name = hasText(surplus.getAssetName()) ? surplus.getAssetName() : "未命名实物";
        String location = hasText(surplus.getInventoryWarehouseName())
                ? surplus.getInventoryWarehouseName() : "未指定仓库";
        if (hasText(surplus.getInventoryLocationName())) {
            location += " · " + surplus.getInventoryLocationName();
        }
        return name + " · " + location;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static final class SurplusViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventorySurplusBinding binding;

        SurplusViewHolder(ItemInventorySurplusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
