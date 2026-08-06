package com.ruoyi.asset.pda.feature.borrow;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ItemBorrowerBinding;

import java.util.ArrayList;
import java.util.List;

/** 人员结果整行可点，保留账号和部门供现场核对同名人员。 */
public final class BorrowerAdapter extends RecyclerView.Adapter<BorrowerAdapter.Holder> {
    public interface Listener {
        void onSelect(PdaMasterDataDto borrower);
    }

    private final Listener listener;
    private final List<PdaMasterDataDto> values = new ArrayList<>();

    public BorrowerAdapter(Listener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("借用人员监听器不能为空");
        }
        this.listener = listener;
    }

    public void submit(List<PdaMasterDataDto> borrowers) {
        int previousSize = values.size();
        values.clear();
        if (previousSize > 0) {
            notifyItemRangeRemoved(0, previousSize);
        }
        if (borrowers != null) {
            values.addAll(borrowers);
        }
        if (!values.isEmpty()) {
            notifyItemRangeInserted(0, values.size());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemBorrowerBinding.inflate(
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
        private final ItemBorrowerBinding binding;

        private Holder(ItemBorrowerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bind(PdaMasterDataDto borrower) {
            binding.borrowerName.setText(value(borrower.getName()));
            binding.borrowerLogin.setText(itemView.getContext().getString(
                    R.string.borrower_login_format, value(borrower.getCode())));
            binding.borrowerDept.setText(itemView.getContext().getString(
                    R.string.borrower_dept_format, value(borrower.getParentName())));
            binding.getRoot().setOnClickListener(view -> listener.onSelect(borrower));
        }

        private String value(String value) {
            return value == null || value.trim().isEmpty()
                    ? itemView.getContext().getString(R.string.common_unknown)
                    : value.trim();
        }
    }
}
