package com.ruoyi.asset.pda.feature.repair;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.databinding.ItemRepairOrderBinding;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 工单列表只提供进入详情的入口，所有可写操作都在详情页重新校验。 */
final class RepairOrderAdapter extends RecyclerView.Adapter<RepairOrderAdapter.ViewHolder> {
    interface OnOrderClickListener {
        void onOrderClick(PdaRepairOrderDto order);
    }

    private final List<PdaRepairOrderDto> items = new ArrayList<>();
    private final OnOrderClickListener listener;
    private List<PdaDictItemDto> statuses = Collections.emptyList();

    RepairOrderAdapter(OnOrderClickListener listener) {
        this.listener = listener;
    }

    void submit(List<PdaRepairOrderDto> orders, List<PdaDictItemDto> statusOptions) {
        items.clear();
        if (orders != null) {
            items.addAll(orders);
        }
        statuses = statusOptions == null ? Collections.emptyList() : statusOptions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRepairOrderBinding binding = ItemRepairOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRepairOrderBinding binding;

        private ViewHolder(ItemRepairOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(PdaRepairOrderDto order) {
            binding.repairOrderRail.setBackgroundColor(ContextCompat.getColor(binding.getRoot().getContext(),
                    RepairUi.statusRailColor(order.getOrderStatus())));
            binding.repairOrderNo.setText(RepairUi.displayText(order.getRepairNo()));
            binding.repairOrderStatus.setText(RepairUi.statusLabel(order.getOrderStatus(), statuses));
            binding.repairOrderAsset.setText(RepairUi.displayText(order.getAssetName()));
            binding.repairOrderAssetCode.setText(RepairUi.displayText(order.getAssetCode()));
            binding.repairOrderFault.setText(RepairUi.displayText(order.getFaultDesc()));
            binding.repairOrderReportTime.setText(RepairUi.displayText(order.getReportTime()));
            binding.getRoot().setOnClickListener(view -> listener.onOrderClick(order));
            binding.repairOrderDetailButton.setOnClickListener(view -> listener.onOrderClick(order));
        }
    }
}
