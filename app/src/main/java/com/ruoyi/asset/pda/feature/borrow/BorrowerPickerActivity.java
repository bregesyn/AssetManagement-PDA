package com.ruoyi.asset.pda.feature.borrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ActivityBorrowerPickerBinding;

/** 借还专用人员搜索页，避免改动现有领用人员选择流程。 */
public final class BorrowerPickerActivity extends SessionAwareActivity {
    public static final String EXTRA_BORROWER_ID = "borrower_id";
    public static final String EXTRA_BORROWER_CODE = "borrower_code";
    public static final String EXTRA_BORROWER_NAME = "borrower_name";
    public static final String EXTRA_BORROWER_DEPT_ID = "borrower_dept_id";
    public static final String EXTRA_BORROWER_DEPT_NAME = "borrower_dept_name";
    public static final String EXTRA_BORROWER_PHONE = "borrower_phone";

    private ActivityBorrowerPickerBinding binding;
    private BorrowerPickerViewModel viewModel;
    private BorrowerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBorrowerPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new BorrowerPickerViewModel.Factory(container.getBorrowRepository()))
                .get(BorrowerPickerViewModel.class);
        adapter = new BorrowerAdapter(this::finishWithBorrower);
        binding.borrowerList.setLayoutManager(new LinearLayoutManager(this));
        binding.borrowerList.setAdapter(adapter);
        binding.borrowerToolbar.setNavigationOnClickListener(view -> finish());
        binding.borrowerSearchButton.setOnClickListener(view -> search());
        binding.borrowerKeywordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                search();
                return true;
            }
            return false;
        });
        viewModel.getUiState().observe(this, this::render);
    }

    private void search() {
        CharSequence value = binding.borrowerKeywordInput.getText();
        viewModel.search(value == null ? null : value.toString());
    }

    private void render(BorrowerPickerUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean searching = state.getMode() == BorrowerPickerUiState.Mode.SEARCHING;
        boolean error = state.getMode() == BorrowerPickerUiState.Mode.ERROR;
        adapter.submit(state.getBorrowers());
        binding.borrowerKeywordInput.setEnabled(!searching);
        binding.borrowerSearchButton.setEnabled(!searching);
        binding.borrowerSearchButton.setText(searching
                ? R.string.borrower_searching : R.string.borrower_search);
        setVisible(binding.borrowerProgress, searching);
        setVisible(binding.borrowerMessageText, error && hasText(state.getMessage()));
        binding.borrowerMessageText.setText(value(state.getMessage()));
        boolean empty = state.getBorrowers().isEmpty() && !searching;
        setVisible(binding.borrowerEmptyText, empty && !error);
        if (empty && !error) {
            binding.borrowerEmptyText.setText(state.getMode()
                    == BorrowerPickerUiState.Mode.CONTENT
                    ? R.string.borrower_no_results : R.string.borrower_picker_empty);
        }
    }

    private void finishWithBorrower(PdaMasterDataDto borrower) {
        if (borrower == null || borrower.getId() == null || borrower.getId() < 1L
                || borrower.getParentId() == null || borrower.getParentId() < 1L) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_BORROWER_ID, borrower.getId());
        result.putExtra(EXTRA_BORROWER_CODE, borrower.getCode());
        result.putExtra(EXTRA_BORROWER_NAME, borrower.getName());
        result.putExtra(EXTRA_BORROWER_DEPT_ID, borrower.getParentId());
        result.putExtra(EXTRA_BORROWER_DEPT_NAME, borrower.getParentName());
        result.putExtra(EXTRA_BORROWER_PHONE, borrower.getPhoneNumber());
        setResult(RESULT_OK, result);
        finish();
    }

    private String value(String value) {
        return hasText(value) ? value.trim() : getString(R.string.common_unknown);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
