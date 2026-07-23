package com.ruoyi.asset.pda.feature.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.databinding.ItemInventoryTaskBinding;

import java.util.ArrayList;
import java.util.List;

public final class InventoryTaskAdapter
        extends RecyclerView.Adapter<InventoryTaskAdapter.TaskViewHolder> {
    public interface Listener {
        void onTaskClicked(PdaInventoryTaskDto task);
    }

    private final Listener listener;
    private final List<PdaInventoryTaskDto> tasks = new ArrayList<>();
    private boolean readonly;

    public InventoryTaskAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<PdaInventoryTaskDto> values, boolean readonly) {
        tasks.clear();
        if (values != null) {
            tasks.addAll(values);
        }
        this.readonly = readonly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(ItemInventoryTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        PdaInventoryTaskDto task = tasks.get(position);
        holder.binding.inventoryTaskNo.setText(valueOrDash(task.getTaskNo()));
        holder.binding.inventoryTaskScope.setText(formatScope(task));
        holder.binding.inventoryTaskProgress.setText(holder.binding.getRoot().getContext().getString(
                R.string.inventory_task_progress_format, task.getInventoriedCount(), task.getTotalCount()));
        holder.binding.inventoryTaskStatus.setText(statusLabel(task));
        holder.binding.inventoryTaskAction.setText(readonly
                ? R.string.inventory_task_readonly_action
                : ("INVENTORYING".equals(task.getTaskStatus())
                        ? R.string.inventory_task_continue_action
                        : R.string.inventory_task_start_action));
        holder.binding.inventoryTaskRail.setBackgroundColor(holder.binding.getRoot().getContext()
                .getColor(readonly ? R.color.pda_primary : R.color.pda_pending));
        holder.binding.getRoot().setOnClickListener(view -> {
            if (listener != null) {
                listener.onTaskClicked(task);
            }
        });
        holder.binding.inventoryTaskAction.setOnClickListener(view -> {
            if (listener != null) {
                listener.onTaskClicked(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private String formatScope(PdaInventoryTaskDto task) {
        StringBuilder builder = new StringBuilder();
        if (hasText(task.getWarehouseName())) {
            builder.append(task.getWarehouseName());
        } else {
            builder.append("未指定仓库");
        }
        if (hasText(task.getLocationName())) {
            builder.append(" · ").append(task.getLocationName());
        }
        if (hasText(task.getCategoryName())) {
            builder.append(" · ").append(task.getCategoryName());
        } else if ("ALL_ASSET".equals(task.getScopeType())) {
            builder.append(" · 全部资产");
        }
        return builder.toString();
    }

    private String statusLabel(PdaInventoryTaskDto task) {
        if ("ISSUED".equals(task.getTaskStatus())) {
            return "待开始";
        }
        if ("INVENTORYING".equals(task.getTaskStatus())) {
            return "盘点中";
        }
        if ("PENDING_RESULT_CONFIRM".equals(task.getTaskStatus())) {
            return "结果待确认";
        }
        return valueOrDash(task.getTaskStatus());
    }

    private String valueOrDash(String value) {
        return hasText(value) ? value : "-";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static final class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventoryTaskBinding binding;

        TaskViewHolder(ItemInventoryTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
