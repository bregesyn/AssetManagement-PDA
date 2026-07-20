package com.ruoyi.asset.pda.feature.rfid;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.databinding.ActivityRfidOperationBinding;

public final class RfidUnbindActivity extends SessionAwareActivity {
    private ActivityRfidOperationBinding binding;
    private RfidUnbindViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRfidOperationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) return;
        viewModel = new ViewModelProvider(this,
                new RfidUnbindViewModel.Factory(
                        container.getRfidRepository(), container.getUhfScanner()))
                .get(RfidUnbindViewModel.class);
        scanKeyDispatcher = new ScanKeyDispatcher(this,
                new ScanKeyDispatcher.Listener() {
                    @Override public void onScanKeyDown() { viewModel.onScanKeyDown(); }
                    @Override public void onScanKeyUp() { viewModel.onScanKeyUp(); }
                });
        configureUi();
        bindActions();
        viewModel.getUiState().observe(this, this::render);
    }

    private void configureUi() {
        binding.operationToolbar.setTitle(R.string.unbind_title);
        binding.operationSubtitleText.setText(R.string.unbind_subtitle);
        binding.operationScanHintText.setText(R.string.unbind_scan_hint);
        binding.operationSubmitButton.setText(R.string.unbind_action);
        setVisible(binding.operationAssetInputContainer, false);
        setVisible(binding.operationAssetCard, false);
    }

    private void bindActions() {
        binding.operationToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.operationScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.operationSubmitButton.setOnClickListener(view -> confirmUnbind());
        binding.operationContinueButton.setOnClickListener(view -> viewModel.reset());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleBack(); }
        });
    }

    private void confirmUnbind() {
        RfidUnbindUiState state = viewModel.getUiState().getValue();
        if (state == null || !state.canUnbind()) return;
        PdaRfidTagDto tag = state.getTag();
        new AlertDialog.Builder(this)
                .setTitle(R.string.unbind_confirm_title)
                .setMessage(getString(R.string.unbind_confirm_message,
                        tag.getEpcCode(), displayValue(tag.getAssetCode())))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm,
                        (dialog, which) -> viewModel.unbind())
                .show();
    }

    private void handleBack() {
        RfidUnbindUiState state = viewModel == null ? null : viewModel.getUiState().getValue();
        if (state == null || !state.isBusy()) finish();
    }

    private void render(RfidUnbindUiState state) {
        if (state == null || binding == null) return;
        boolean busy = state.isBusy();
        boolean success = state.isSuccess();
        PdaRfidTagDto tag = state.getTag();

        binding.operationScanButton.setEnabled(!busy && !success);
        binding.operationScanButton.setText(state.isScanning()
                ? R.string.common_scan_stop : R.string.common_scan_start);
        binding.operationScanStatusText.setText(scanStateText(state.getScanState()));
        setVisible(binding.operationTagCard, tag != null);
        if (tag != null) renderTag(tag);

        boolean notBound = tag != null && !tag.isRfidBound() && !success;
        setVisible(binding.operationNoticeText, notBound);
        if (notBound) binding.operationNoticeText.setText(R.string.unbind_not_bound);
        binding.operationSubmitButton.setEnabled(state.canUnbind());
        setVisible(binding.operationSubmitButton, !success);

        setVisible(binding.operationProgress, busy);
        setVisible(binding.operationProgressText, busy);
        if (busy) binding.operationProgressText.setText(progressText(state));
        String error = state.getErrorTextResId() == 0
                ? state.getErrorMessage() : getString(state.getErrorTextResId());
        setVisible(binding.operationErrorText, hasText(error));
        binding.operationErrorText.setText(error);
        setVisible(binding.operationSuccessText, success);
        binding.operationSuccessText.setText(R.string.unbind_success);
        setVisible(binding.operationContinueButton, success);
    }

    private void renderTag(PdaRfidTagDto tag) {
        binding.operationTagCodeText.setText(getString(
                R.string.rfid_tag_code_format, displayValue(tag.getTagCode())));
        binding.operationEpcText.setText(getString(
                R.string.rfid_epc_format, displayValue(tag.getEpcCode())));
        binding.operationTagStatusText.setText(getString(
                R.string.rfid_tag_status_format, displayValue(tag.getTagStatusName())));
        binding.operationBindStatusText.setText(getString(
                R.string.rfid_bind_status_format, displayValue(tag.getBindStatusName())));
        binding.operationBoundAssetText.setText(tag.isRfidBound()
                ? getString(R.string.rfid_bound_asset_format,
                        displayValue(tag.getAssetName()), displayValue(tag.getAssetCode()))
                : getString(R.string.rfid_no_bound_asset));
    }

    private int progressText(RfidUnbindUiState state) {
        if (state.isLoadingTag()) return R.string.rfid_operation_loading_tag;
        if (state.isVerifying()) return R.string.rfid_operation_verifying;
        return R.string.rfid_operation_submitting;
    }

    private int scanStateText(UhfScanState state) {
        if (state == UhfScanState.PROCESSING) return R.string.common_scan_starting;
        if (state == UhfScanState.SCANNING) return R.string.common_scan_active;
        if (state == UhfScanState.ERROR) return R.string.common_scan_error;
        return R.string.common_scan_idle;
    }

    private String displayValue(String value) {
        return hasText(value) ? value.trim() : getString(R.string.common_unknown);
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
