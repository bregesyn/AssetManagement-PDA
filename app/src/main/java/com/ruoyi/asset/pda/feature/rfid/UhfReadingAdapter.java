package com.ruoyi.asset.pda.feature.rfid;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.databinding.ItemUhfTagReadingBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class UhfReadingAdapter extends RecyclerView.Adapter<UhfReadingAdapter.Holder> {
    private List<UhfTagReading> items = Collections.emptyList();

    void submit(List<UhfTagReading> values) {
        List<UhfTagReading> next = values == null
                ? Collections.emptyList() : new ArrayList<>(values);
        int previousSize = items.size();
        if (isPureAppend(items, next)) {
            items = next;
            notifyItemRangeInserted(previousSize, next.size() - previousSize);
            return;
        }
        if (previousSize == next.size()) {
            List<UhfTagReading> previous = items;
            items = next;
            for (int index = 0; index < next.size(); index++) {
                if (!sameContent(previous.get(index), next.get(index))) {
                    notifyItemChanged(index);
                }
            }
            return;
        }
        items = Collections.emptyList();
        if (previousSize > 0) notifyItemRangeRemoved(0, previousSize);
        items = next;
        if (!next.isEmpty()) notifyItemRangeInserted(0, next.size());
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemUhfTagReadingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private boolean isPureAppend(List<UhfTagReading> previous,
            List<UhfTagReading> next) {
        if (next.size() <= previous.size()) return false;
        for (int index = 0; index < previous.size(); index++) {
            if (!sameContent(previous.get(index), next.get(index))) return false;
        }
        return true;
    }

    private boolean sameContent(UhfTagReading first, UhfTagReading second) {
        return first.getEpc().equals(second.getEpc())
                && first.getRssi() == second.getRssi()
                && first.getReadCount() == second.getReadCount()
                && first.getFirstSeenAt() == second.getFirstSeenAt()
                && first.getLastSeenAt() == second.getLastSeenAt();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemUhfTagReadingBinding binding;

        Holder(ItemUhfTagReadingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(UhfTagReading reading) {
            binding.readingEpcText.setText(itemView.getContext().getString(
                    R.string.batch_row_epc, reading.getEpc()));
            binding.readingStatsText.setText(itemView.getContext().getString(
                    R.string.batch_row_reading, reading.getRssi(),
                    reading.getReadCount()));
        }
    }
}
