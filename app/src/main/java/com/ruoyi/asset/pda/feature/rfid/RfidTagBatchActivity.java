package com.ruoyi.asset.pda.feature.rfid;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;
import com.ruoyi.asset.pda.databinding.ActivityRfidTagBatchBinding;

public final class RfidTagBatchActivity extends SessionAwareActivity {
    private ActivityRfidTagBatchBinding binding;
    private RfidTagBatchViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;
    private final UhfReadingAdapter readingAdapter = new UhfReadingAdapter();
    private final RfidBatchResultAdapter resultAdapter = new RfidBatchResultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRfidTagBatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new RfidTagBatchViewModel.Factory(
                        container.getRfidRepository(), container.getUhfScanner()))
                .get(RfidTagBatchViewModel.class);
        scanKeyDispatcher = new ScanKeyDispatcher(this,
                new ScanKeyDispatcher.Listener() {
                    @Override
                    public void onScanKeyPressed() { viewModel.onScanKeyPressed(); }
                });
        binding.batchReadingsList.setLayoutManager(new LinearLayoutManager(this));
        binding.batchReadingsList.setAdapter(readingAdapter);
        binding.batchResultList.setLayoutManager(new LinearLayoutManager(this));
        binding.batchResultList.setAdapter(resultAdapter);
        bindActions();
        viewModel.getUiState().observe(this, this::render);
    }

    private void bindActions() {
        binding.batchToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.batchScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.batchSubmitButton.setOnClickListener(view -> viewModel.submit());
        binding.batchContinueButton.setOnClickListener(view -> viewModel.startNewBatch());
        binding.batchRemarkEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setRemark(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void handleBack() {
        if (viewModel == null || viewModel.isSubmitting()) {
            return;
        }
        if (!viewModel.hasUnsubmittedReadings()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.batch_leave_title)
                .setMessage(R.string.batch_leave_message)
                .setNegativeButton(R.string.batch_leave_cancel, null)
                .setPositiveButton(R.string.batch_leave_confirm,
                        (dialog, which) -> finish())
                .show();
    }

    private void render(RfidTagBatchUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean resultMode = state.isResultMode();
        boolean submitting = state.isSubmitting();
        setVisible(binding.batchCaptureContainer, !resultMode);
        setVisible(binding.batchResultContainer, resultMode);
        setVisible(binding.batchProgress, submitting);
        binding.batchScanStatusText.setText(scanStateText(state.getScanState()));
        binding.batchScanButton.setText(state.isScanning()
                ? R.string.common_scan_stop : R.string.common_scan_start);
        binding.batchScanButton.setEnabled(!submitting);
        binding.batchSubmitButton.setEnabled(!submitting && !state.isScanning()
                && !state.getReadings().isEmpty());
        binding.batchRemarkEditText.setEnabled(!submitting);
        binding.batchUniqueText.setText(getString(
                R.string.batch_unique_format, state.getReadings().size()));
        binding.batchDuplicateText.setText(getString(
                R.string.batch_duplicate_format, state.getDuplicateReadCount()));

        UhfTagReading last = state.getLastReading();
        setVisible(binding.batchLastText, last != null);
        if (last != null) {
            binding.batchLastText.setText(getString(R.string.batch_last_format,
                    last.getEpc(), last.getRssi(), last.getReadCount()));
        }
        readingAdapter.submit(state.getReadings());
        setVisible(binding.batchEmptyText, state.getReadings().isEmpty());
        setVisible(binding.batchReadingsList, !state.getReadings().isEmpty());
        CharSequence currentRemark = binding.batchRemarkEditText.getText();
        if (currentRemark == null || !currentRemark.toString().equals(state.getRemark())) {
            binding.batchRemarkEditText.setText(state.getRemark());
            binding.batchRemarkEditText.setSelection(state.getRemark().length());
        }

        RfidTagBatchResultDto result = state.getResult();
        if (result != null) {
            binding.batchResultSummaryText.setText(getString(R.string.batch_result_summary,
                    result.getSuccessCount(), result.getDuplicateCount(),
                    result.getFailureCount()));
            resultAdapter.submit(result.getRows());
        }
        String error = state.getErrorTextResId() == 0
                ? state.getErrorMessage() : getString(state.getErrorTextResId());
        setVisible(binding.batchErrorText, hasText(error));
        binding.batchErrorText.setText(error);
    }

    private int scanStateText(UhfScanState state) {
        if (state == UhfScanState.PROCESSING) return R.string.common_scan_starting;
        if (state == UhfScanState.SCANNING) return R.string.common_scan_active;
        if (state == UhfScanState.ERROR) return R.string.common_scan_error;
        return R.string.common_scan_idle;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (scanKeyDispatcher != null) scanKeyDispatcher.start();
    }

    @Override
    protected void onStop() {
        if (scanKeyDispatcher != null) scanKeyDispatcher.stop();
        if (viewModel != null) viewModel.releaseScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
