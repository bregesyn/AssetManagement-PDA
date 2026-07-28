package com.ruoyi.asset.pda.feature.receive;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ItemReceiveRecipientBinding;

import java.util.ArrayList;
import java.util.List;

/** 人员结果每行可直接选择，保留登录名供现场核对同名人员。 */
public final class ReceiveRecipientAdapter
        extends RecyclerView.Adapter<ReceiveRecipientAdapter.Holder> {
    public interface Listener {
        void onSelect(PdaMasterDataDto recipient);
    }

    private final Listener listener;
    private final List<PdaMasterDataDto> values = new ArrayList<>();

    public ReceiveRecipientAdapter(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("领用人员监听器不能为空");
        }
        this.listener = listener;
    }

    public void submit(List<PdaMasterDataDto> items) {
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
        return new Holder(ItemReceiveRecipientBinding.inflate(
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
        private final ItemReceiveRecipientBinding binding;

        private Holder(ItemReceiveRecipientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(PdaMasterDataDto recipient) {
            binding.receiveRecipientName.setText(value(recipient.getName()));
            binding.receiveRecipientLogin.setText(itemView.getContext().getString(
                    R.string.receive_recipient_login_format, value(recipient.getCode())));
            binding.receiveRecipientDept.setText(itemView.getContext().getString(
                    R.string.receive_recipient_dept_format,
                    value(recipient.getParentName())));
            binding.getRoot().setOnClickListener(view -> listener.onSelect(recipient));
        }

        private String value(String value) {
            return value == null || value.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.common_unknown)
                    : value.trim();
        }
    }
}
