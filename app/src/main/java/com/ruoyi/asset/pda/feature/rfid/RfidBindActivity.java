package com.ruoyi.asset.pda.feature.rfid;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.databinding.ActivityRfidOperationBinding;

public final class RfidBindActivity extends SessionAwareActivity {
    private ActivityRfidOperationBinding binding;
    private RfidBindViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRfidOperationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new RfidBindViewModel.Factory(container.getAssetRepository(),
                        container.getRfidRepository(), container.getUhfScanner()))
                .get(RfidBindViewModel.class);
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
        binding.operationToolbar.setTitle(R.string.bind_title);
        binding.operationSubtitleText.setText(R.string.bind_subtitle);
        binding.operationScanHintText.setText(R.string.bind_scan_hint);
        binding.operationSubmitButton.setText(R.string.bind_action);
        setVisible(binding.operationAssetInputContainer, true);
    }

    private void bindActions() {
        binding.operationToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.operationAssetQueryButton.setOnClickListener(view -> queryAsset());
        binding.operationAssetCodeEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                viewModel.onAssetCodeChanged(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.operationAssetCodeEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                queryAsset();
                return true;
            }
            return false;
        });
        binding.operationScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.operationSubmitButton.setOnClickListener(view -> confirmBind());
        binding.operationContinueButton.setOnClickListener(view -> {
            binding.operationAssetCodeEditText.setText("");
            viewModel.reset();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleBack(); }
        });
    }

    private void queryAsset() {
        CharSequence text = binding.operationAssetCodeEditText.getText();
        viewModel.queryAsset(text == null ? "" : text.toString());
    }

    private void confirmBind() {
        RfidBindUiState state = viewModel.getUiState().getValue();
        if (state == null || !state.canBind()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.bind_confirm_title)
                .setMessage(getString(R.string.bind_confirm_message,
                        state.getAsset().getAssetCode(), state.getTag().getEpcCode()))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm,
                        (dialog, which) -> viewModel.bind())
                .show();
    }

    private void handleBack() {
        RfidBindUiState state = viewModel == null ? null : viewModel.getUiState().getValue();
        if (state == null || !state.isBusy()) finish();
    }

    private void render(RfidBindUiState state) {
        if (state == null || binding == null) return;
        boolean busy = state.isBusy();
        boolean success = state.isSuccess();
        PdaAssetIdentifyDto asset = state.getAsset();
        PdaRfidTagDto tag = state.getTag();

        binding.operationAssetCodeEditText.setEnabled(!busy && !success
                && !state.isScanning());
        binding.operationAssetQueryButton.setEnabled(!busy && !success
                && !state.isScanning());
        binding.operationScanButton.setEnabled(!busy && !success && asset != null
                && !asset.isRfidBound());
        binding.operationScanButton.setText(state.isScanning()
                ? R.string.common_scan_stop : R.string.common_scan_start);
        binding.operationScanStatusText.setText(scanStateText(state.getScanState()));

        setVisible(binding.operationAssetCard, asset != null);
        if (asset != null) renderAsset(asset);
        setVisible(binding.operationTagCard, tag != null);
        if (tag != null) renderTag(tag);

        boolean assetUnavailable = asset != null && asset.isRfidBound();
        boolean tagUnavailable = tag != null && !tag.isNormalAndUnbound();
        setVisible(binding.operationNoticeText, assetUnavailable || tagUnavailable);
        if (assetUnavailable) {
            binding.operationNoticeText.setText(R.string.bind_asset_already_bound);
        } else if (tagUnavailable) {
            binding.operationNoticeText.setText(R.string.bind_tag_unavailable);
        }

        binding.operationSubmitButton.setEnabled(state.canBind());
        setVisible(binding.operationSubmitButton, !success);
        setVisible(binding.operationProgress, busy);
        setVisible(binding.operationProgressText, busy);
        if (busy) binding.operationProgressText.setText(progressText(state));

        String error = state.getErrorTextResId() == 0
                ? state.getErrorMessage() : getString(state.getErrorTextResId());
        setVisible(binding.operationErrorText, hasText(error));
        binding.operationErrorText.setText(error);
        setVisible(binding.operationSuccessText, success);
        binding.operationSuccessText.setText(R.string.bind_success);
        setVisible(binding.operationContinueButton, success);
    }

    private void renderAsset(PdaAssetIdentifyDto asset) {
        binding.operationAssetTitleText.setText(getString(R.string.bind_asset_title_format,
                displayValue(asset.getAssetName()), displayValue(asset.getAssetCode())));
        binding.operationAssetCategoryText.setText(getString(
                R.string.bind_asset_category_format, displayValue(asset.getCategoryName())));
        binding.operationAssetStatusText.setText(getString(
                R.string.bind_asset_status_format, displayValue(asset.getAssetStatusName())));
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

    private int progressText(RfidBindUiState state) {
        if (state.isLoadingAsset()) return R.string.rfid_operation_loading_asset;
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
