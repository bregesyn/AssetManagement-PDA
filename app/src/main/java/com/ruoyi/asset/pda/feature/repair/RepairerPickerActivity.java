package com.ruoyi.asset.pda.feature.repair;

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
import com.ruoyi.asset.pda.data.dto.PdaRepairerDto;
import com.ruoyi.asset.pda.databinding.ActivityRepairerPickerBinding;

/** 单独的内部维修人搜索页，避免在开始维修页塞入难以戴手套操作的下拉列表。 */
public final class RepairerPickerActivity extends SessionAwareActivity {
    public static final String EXTRA_REPAIRER_ID = "repairer_id";
    public static final String EXTRA_REPAIRER_NAME = "repairer_name";
    public static final String EXTRA_REPAIRER_CODE = "repairer_code";

    private ActivityRepairerPickerBinding binding;
    private RepairerPickerViewModel viewModel;
    private RepairerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairerPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new RepairerPickerViewModel.Factory(container.getRepairRepository()))
                .get(RepairerPickerViewModel.class);
        adapter = new RepairerAdapter(this::finishWithRepairer);
        binding.repairerList.setLayoutManager(new LinearLayoutManager(this));
        binding.repairerList.setAdapter(adapter);
        binding.repairerToolbar.setNavigationOnClickListener(view -> finish());
        binding.repairerSearchButton.setOnClickListener(view -> search());
        binding.repairerKeywordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                search();
                return true;
            }
            return false;
        });
        viewModel.getState().observe(this, this::render);
    }

    private void search() {
        CharSequence value = binding.repairerKeywordInput.getText();
        viewModel.search(value == null ? null : value.toString());
    }

    private void render(RepairerPickerUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean searching = state.getMode() == RepairerPickerUiState.Mode.SEARCHING;
        boolean error = state.getMode() == RepairerPickerUiState.Mode.ERROR;
        adapter.submit(state.getRepairers());
        binding.repairerKeywordInput.setEnabled(!searching);
        binding.repairerSearchButton.setEnabled(!searching);
        binding.repairerSearchButton.setText(searching ? R.string.repairer_searching
                : R.string.repairer_search);
        setVisible(binding.repairerProgress, searching);
        setVisible(binding.repairerMessage, error && RepairUi.hasText(state.getMessage()));
        binding.repairerMessage.setText(state.getMessage());
        boolean empty = state.getRepairers().isEmpty() && !searching && !error;
        setVisible(binding.repairerEmpty, empty);
        if (empty) {
            binding.repairerEmpty.setText(state.getMode() == RepairerPickerUiState.Mode.CONTENT
                    ? R.string.repairer_no_results : R.string.repairer_picker_empty);
        }
    }

    private void finishWithRepairer(PdaRepairerDto repairer) {
        if (repairer == null || repairer.getId() == null || repairer.getId() < 1L) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_REPAIRER_ID, repairer.getId());
        result.putExtra(EXTRA_REPAIRER_NAME, repairer.getName());
        result.putExtra(EXTRA_REPAIRER_CODE, repairer.getCode());
        setResult(RESULT_OK, result);
        finish();
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
