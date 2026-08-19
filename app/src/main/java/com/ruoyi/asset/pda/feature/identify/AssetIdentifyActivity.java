package com.ruoyi.asset.pda.feature.identify;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.databinding.ActivityAssetIdentifyBinding;

public final class AssetIdentifyActivity extends SessionAwareActivity {
    private ActivityAssetIdentifyBinding binding;
    private AssetIdentifyViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssetIdentifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new AssetIdentifyViewModel.Factory(
                        container.getAssetRepository(), container.getUhfScanner()))
                .get(AssetIdentifyViewModel.class);
        scanKeyDispatcher = new ScanKeyDispatcher(this,
                new ScanKeyDispatcher.Listener() {
                    @Override
                    public void onScanKeyPressed() {
                        viewModel.onScanKeyPressed();
                    }
                });
        bindActions();
        viewModel.getUiState().observe(this, this::render);
    }

    private void bindActions() {
        binding.identifyToolbar.setNavigationOnClickListener(view -> finish());
        binding.identifyTypeGroup.setOnCheckedChangeListener((group, checkedId) ->
                viewModel.selectIdentifyType(checkedId == R.id.identifyEpcRadio
                        ? AssetRepository.IDENTIFY_TYPE_EPC
                        : AssetRepository.IDENTIFY_TYPE_ASSET_CODE));
        binding.identifyScanButton.setOnClickListener(view -> viewModel.toggleScan());
        binding.identifyAssetCodeButton.setOnClickListener(view -> submitAssetCode());
        binding.identifyAssetCodeEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitAssetCode();
                return true;
            }
            return false;
        });
    }

    private void submitAssetCode() {
        CharSequence text = binding.identifyAssetCodeEditText.getText();
        viewModel.identifyAssetCode(text == null ? "" : text.toString());
    }

    private void render(AssetIdentifyUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean epcMode = state.isEpcMode();
        int checkedId = epcMode ? R.id.identifyEpcRadio : R.id.identifyAssetCodeRadio;
        if (binding.identifyTypeGroup.getCheckedRadioButtonId() != checkedId) {
            binding.identifyTypeGroup.check(checkedId);
        }
        setVisible(binding.identifyEpcContainer, epcMode);
        setVisible(binding.identifyAssetCodeContainer, !epcMode);
        binding.identifyTypeGroup.setEnabled(!state.isLoading());
        binding.identifyAssetCodeEditText.setEnabled(!state.isLoading());
        binding.identifyAssetCodeButton.setEnabled(!state.isLoading());
        binding.identifyScanButton.setEnabled(!state.isLoading());
        binding.identifyScanButton.setText(state.isScanning()
                ? R.string.identify_scan_stop : R.string.identify_scan_start);
        binding.identifyScanStatusText.setText(scanStateText(state.getScanState()));
        binding.identifyScanCountText.setText(scanCountText(state));
        boolean showCaption = state.isScanning() || state.isLoading();
        setVisible(binding.identifyScanCaptionText, showCaption);
        if (showCaption) {
            binding.identifyScanCaptionText.setText(R.string.identify_scan_caption_active);
        }
        setVisible(binding.identifyProgress, state.isLoading());

        // 识别成功后 EPC 已在“最近一次识别”卡片中展示；仅在未匹配资产时保留，便于现场复核失败标签。
        boolean hasLastEpc = state.getAsset() == null && hasText(state.getLastEpc());
        setVisible(binding.identifyLastEpcText, hasLastEpc);
        if (hasLastEpc) {
            binding.identifyLastEpcText.setText(
                    getString(R.string.identify_last_epc_format, state.getLastEpc()));
        }

        String error = state.getErrorTextResId() == 0
                ? state.getErrorMessage() : getString(state.getErrorTextResId());
        setVisible(binding.identifyErrorText, hasText(error));
        binding.identifyErrorText.setText(error);

        PdaAssetIdentifyDto asset = state.getAsset();
        setVisible(binding.identifyResultCard, asset != null);
        if (asset != null) {
            renderAsset(asset);
        }
    }

    private void renderAsset(PdaAssetIdentifyDto asset) {
        boolean rfidBound = asset.isRfidBound();
        binding.identifyResultStatusText.setText(rfidBound
                ? R.string.identify_rfid_bound : R.string.identify_rfid_unbound);
        binding.identifyResultStatusText.setTextColor(getColor(rfidBound
                ? R.color.pda_success : R.color.pda_pending));
        binding.identifyResultStatusText.setBackgroundResource(rfidBound
                ? R.drawable.pda_online_badge : R.drawable.pda_pending_badge);
        binding.identifyEpcText.setText(getString(R.string.identify_epc_format,
                display(asset.getEpcCode())));
        binding.identifyAssetTitleText.setText(getString(R.string.identify_asset_name_format,
                display(asset.getAssetName()), display(asset.getAssetCode())));
        binding.identifyCategoryText.setText(getString(R.string.identify_category_format,
                display(asset.getCategoryName())));
        binding.identifyStatusText.setText(getString(R.string.identify_status_format,
                display(asset.getAssetStatusName())));
        binding.identifyModelBrandText.setText(getString(R.string.identify_model_brand_format,
                display(asset.getSpecModel()), display(asset.getBrand())));
        binding.identifyWarehouseText.setText(getString(R.string.identify_warehouse_format,
                display(asset.getWarehouseName())));
        binding.identifyLocationText.setText(getString(R.string.identify_warehouse_location_format,
                display(asset.getWarehouseName()), display(asset.getLocationName())));
        binding.identifyRfidText.setText(getString(R.string.identify_rfid_format,
                getString(rfidBound
                        ? R.string.identify_rfid_bound : R.string.identify_rfid_unbound),
                display(asset.getTagCode())));
    }

    private int scanStateText(UhfScanState state) {
        if (state == UhfScanState.PROCESSING) {
            return R.string.identify_scan_preparing;
        }
        if (state == UhfScanState.SCANNING) {
            return R.string.identify_scan_active;
        }
        if (state == UhfScanState.ERROR) {
            return R.string.common_scan_error;
        }
        return R.string.identify_scan_ready;
    }

    private int scanCountText(AssetIdentifyUiState state) {
        return state.isScanning() || state.isLoading() || hasText(state.getLastEpc())
                ? R.string.identify_scan_count_one : R.string.identify_scan_count_zero;
    }

    private String display(String value) {
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
        if (scanKeyDispatcher != null) {
            scanKeyDispatcher.start();
        }
    }

    @Override
    protected void onStop() {
        if (scanKeyDispatcher != null) {
            scanKeyDispatcher.stop();
        }
        if (viewModel != null) {
            viewModel.releaseScanner();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
