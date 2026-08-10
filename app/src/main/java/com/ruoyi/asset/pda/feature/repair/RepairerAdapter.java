package com.ruoyi.asset.pda.feature.repair;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.databinding.ItemRepairerBinding;

import java.util.ArrayList;
import java.util.List;

/** 人员项只返回 ID，开始维修请求不会信任本地姓名和电话。 */
final class RepairerAdapter extends RecyclerView.Adapter<RepairerAdapter.ViewHolder> {
    interface OnRepairerClickListener {
        void onRepairerClick(PdaRepairerDto repairer);
    }

    private final List<PdaRepairerDto> items = new ArrayList<>();
    private final OnRepairerClickListener listener;

    RepairerAdapter(OnRepairerClickListener listener) {
        this.listener = listener;
    }

    void submit(List<PdaRepairerDto> repairers) {
        items.clear();
        if (repairers != null) {
            items.addAll(repairers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemRepairerBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false));
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
        private final ItemRepairerBinding binding;

        private ViewHolder(ItemRepairerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(PdaRepairerDto repairer) {
            binding.repairerName.setText(display(repairer.getName()));
            binding.repairerLogin.setText(binding.getRoot().getContext().getString(
                    R.string.repairer_login_format, display(repairer.getCode())));
            binding.repairerDepartment.setText(binding.getRoot().getContext().getString(
                    R.string.repairer_dept_format, display(repairer.getParentName())));
            binding.getRoot().setOnClickListener(view -> listener.onRepairerClick(repairer));
        }

        private String display(String value) {
            return RepairUi.hasText(value) ? value.trim()
                    : binding.getRoot().getContext().getString(R.string.common_unknown);
        }
    }
}
