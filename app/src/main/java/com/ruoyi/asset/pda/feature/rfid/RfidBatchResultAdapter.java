package com.ruoyi.asset.pda.feature.rfid;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchRowDto;
import com.ruoyi.asset.pda.databinding.ItemRfidBatchResultBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class RfidBatchResultAdapter
        extends ListAdapter<RfidTagBatchRowDto, RfidBatchResultAdapter.Holder> {
    private static final DiffUtil.ItemCallback<RfidTagBatchRowDto> ITEM_CALLBACK =
            new DiffUtil.ItemCallback<RfidTagBatchRowDto>() {
                @Override
                public boolean areItemsTheSame(@NonNull RfidTagBatchRowDto oldItem,
                        @NonNull RfidTagBatchRowDto newItem) {
                    return Objects.equals(oldItem.getRowNumber(), newItem.getRowNumber())
                            && Objects.equals(oldItem.getEpcCode(), newItem.getEpcCode());
                }

                @Override
                public boolean areContentsTheSame(@NonNull RfidTagBatchRowDto oldItem,
                        @NonNull RfidTagBatchRowDto newItem) {
                    return oldItem.isSuccess() == newItem.isSuccess()
                            && oldItem.isDuplicate() == newItem.isDuplicate()
                            && Objects.equals(oldItem.getTagId(), newItem.getTagId())
                            && Objects.equals(oldItem.getTagCode(), newItem.getTagCode())
                            && Objects.equals(oldItem.getMessage(), newItem.getMessage());
                }
            };

    RfidBatchResultAdapter() {
        super(ITEM_CALLBACK);
    }

    void submit(List<RfidTagBatchRowDto> values) {
        submitList(values == null ? Collections.emptyList() : new ArrayList<>(values));
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemRfidBatchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemRfidBatchResultBinding binding;

        Holder(ItemRfidBatchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RfidTagBatchRowDto row) {
            int statusRes;
            int colorRes;
            if (row.isSuccess()) {
                statusRes = R.string.batch_row_success;
                colorRes = R.color.pda_primary;
            } else if (row.isDuplicate()) {
                statusRes = R.string.batch_row_duplicate;
                colorRes = R.color.pda_pending;
            } else {
                statusRes = R.string.batch_row_failure;
                colorRes = R.color.pda_error;
            }
            binding.resultStatusText.setText(statusRes);
            @ColorInt int color = ContextCompat.getColor(itemView.getContext(), colorRes);
            binding.resultStatusText.setTextColor(color);
            binding.resultEpcText.setText(itemView.getContext().getString(
                    R.string.batch_row_epc, display(row.getEpcCode())));
            boolean hasTag = hasText(row.getTagCode());
            binding.resultTagText.setVisibility(hasTag ? View.VISIBLE : View.GONE);
            if (hasTag) {
                binding.resultTagText.setText(itemView.getContext().getString(
                        R.string.batch_row_tag, row.getTagCode()));
            }
            binding.resultMessageText.setText(display(row.getMessage()));
        }

        private String display(String value) {
            return hasText(value) ? value.trim()
                    : itemView.getContext().getString(R.string.common_unknown);
        }

        private boolean hasText(String value) {
            return value != null && !value.trim().isEmpty();
        }
    }
}
