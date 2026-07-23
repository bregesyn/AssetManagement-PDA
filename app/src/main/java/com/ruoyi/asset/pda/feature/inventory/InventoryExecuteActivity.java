package com.ruoyi.asset.pda.feature.inventory;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.uhf.ScanKeyDispatcher;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ActivityInventoryExecuteBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 盘点执行页只承担交互编排：应盘清单始终可见，批量预判与确认仍全部交由 ViewModel。
 */
public final class InventoryExecuteActivity extends SessionAwareActivity {
    public static final String EXTRA_TASK_ID = "inventory_task_id";
    public static final String EXTRA_TASK_NO = "inventory_task_no";
    public static final String EXTRA_CAN_SUBMIT = "inventory_can_submit";
    static final String EXTRA_SKIP_INITIAL_LOAD = "inventory_skip_initial_load";

    private ActivityInventoryExecuteBinding binding;
    private InventoryExecuteViewModel viewModel;
    private ScanKeyDispatcher scanKeyDispatcher;
    private InventoryPreviewAdapter previewAdapter;
    private InventoryItemAdapter itemAdapter;
    private InventorySurplusAdapter surplusAdapter;
    private AlertDialog exceptionDialog;
    private AlertDialog surplusDialog;
    private boolean selectorCallbackReady;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInventoryExecuteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        long taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1L);
        if (taskId < 1) {
            Toast.makeText(this, R.string.inventory_task_invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String taskNo = getIntent().getStringExtra(EXTRA_TASK_NO);
        boolean canSubmit = getIntent().getBooleanExtra(EXTRA_CAN_SUBMIT, false);
        viewModel = new ViewModelProvider(this,
                new InventoryExecuteViewModel.Factory(container.getInventoryRepository(),
                        container.getCommonRepository(), container.getUhfScanner(),
                        taskId, taskNo, canSubmit)).get(InventoryExecuteViewModel.class);

        previewAdapter = new InventoryPreviewAdapter(
                (epcCode, selected) -> viewModel.setPreviewSelection(epcCode, selected));
        itemAdapter = new InventoryItemAdapter(new InventoryItemAdapter.Listener() {
            @Override
            public void onCorrect(PdaInventoryItemDto item) {
                showCorrectionDialog(item);
            }

            @Override
            public void onPreviewSelectionChanged(String epcCode, boolean selected) {
                viewModel.setPreviewSelection(epcCode, selected);
            }
        });
        surplusAdapter = new InventorySurplusAdapter(this::showDeleteSurplusDialog);
        binding.inventoryExpectedList.setLayoutManager(new LinearLayoutManager(this));
        binding.inventoryExpectedList.setAdapter(itemAdapter);
        binding.inventoryExpectedList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    viewModel.loadMoreExpectedItems();
                }
            }
        });

        bindSelectors();
        bindActions();
        viewModel.getUiState().observe(this, this::render);
        // 仪器测试只校验页面骨架；真机仍必须按任务号加载服务端事实。
        if (!BuildConfig.DEBUG || !getIntent().getBooleanExtra(EXTRA_SKIP_INITIAL_LOAD, false)) {
            viewModel.initialize();
        }
    }

    private void bindSelectors() {
        binding.inventoryWarehouseInput.setOnItemClickListener((parent, view, position, id) -> {
            if (!selectorCallbackReady || viewModel == null) {
                return;
            }
            InventoryExecuteUiState state = viewModel.getUiState().getValue();
            if (state == null || position >= state.getWarehouses().size()) {
                return;
            }
            Long nextId = state.getWarehouses().get(position).getId();
            runAfterBatchConfirmation(() -> viewModel.changeWarehouse(nextId));
        });
        binding.inventoryLocationInput.setOnItemClickListener((parent, view, position, id) -> {
            if (!selectorCallbackReady || viewModel == null) {
                return;
            }
            InventoryExecuteUiState state = viewModel.getUiState().getValue();
            if (state == null) {
                return;
            }
            Long nextId = position == 0 || position - 1 >= state.getLocations().size()
                    ? null : state.getLocations().get(position - 1).getId();
            runAfterBatchConfirmation(() -> viewModel.changeLocation(nextId));
        });
        selectorCallbackReady = true;
    }

    private void bindActions() {
        binding.inventoryExecuteToolbar.setNavigationOnClickListener(view -> handleBack());
        binding.inventoryPrimaryAction.setOnClickListener(view -> onPrimaryAction());
        binding.inventorySecondaryAction.setOnClickListener(view -> onSecondaryAction());
        binding.inventoryExceptionBar.setOnClickListener(view -> showExceptionDialog());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void onPrimaryAction() {
        InventoryExecuteUiState state = currentState();
        if (state == null || state.getTask() == null || state.isWriting()) {
            return;
        }
        if (state.isReadOnly()) {
            finish();
            return;
        }
        if (state.getPreview() != null) {
            if (state.isCanSubmit() && !state.getSelectedEpcs().isEmpty()) {
                showConfirmDialog();
            }
            return;
        }
        if (isScanning(state)) {
            viewModel.toggleScan();
            return;
        }
        if (!state.getReadings().isEmpty()) {
            viewModel.precheck();
            return;
        }
        if (state.getTask().getPendingCount() == 0L) {
            if (state.isCanSubmit()) {
                showSubmitDialog();
            }
            return;
        }
        viewModel.toggleScan();
    }

    private void onSecondaryAction() {
        InventoryExecuteUiState state = currentState();
        if (state == null || state.getTask() == null || state.isWriting()) {
            return;
        }
        if (state.isReadOnly()) {
            showSurplusListDialog();
            return;
        }
        if (state.getPreview() != null) {
            showResumeCollectionDialog();
            return;
        }
        if (isScanning(state)) {
            showDiscardBatchDialog();
            return;
        }
        if (!state.getReadings().isEmpty()) {
            viewModel.toggleScan();
            return;
        }
        if (state.getTask().getPendingCount() == 0L) {
            showSurplusListDialog();
        } else if (state.isCanSubmit()) {
            showMarkLossDialog();
        }
    }

    private void render(InventoryExecuteUiState state) {
        if (state == null || binding == null) {
            return;
        }
        PdaInventoryTaskDto task = state.getTask();
        boolean loading = task == null;
        binding.inventoryTaskMeta.setText(loading ? getString(R.string.inventory_execute_loading)
                : valueOrDash(task.getTaskNo()) + " · " + formatScope(task));
        binding.inventoryProgress.setText(loading ? "" : "已盘 " + task.getInventoriedCount()
                + " / " + task.getTotalCount());
        renderSelectors(state);
        renderBatch(state);
        renderExpectedItems(state);
        renderMessages(state);
        renderActions(state);
        renderDialogs(state);
    }

    private void renderSelectors(InventoryExecuteUiState state) {
        List<String> warehouseNames = new ArrayList<>();
        for (PdaMasterDataDto value : state.getWarehouses()) {
            warehouseNames.add(optionLabel(value));
        }
        binding.inventoryWarehouseInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, warehouseNames));
        binding.inventoryWarehouseInput.setText(findOptionLabel(state.getWarehouses(),
                state.getSelectedWarehouseId()), false);

        List<String> locationNames = new ArrayList<>();
        locationNames.add(getString(R.string.inventory_execute_select_location));
        for (PdaMasterDataDto value : state.getLocations()) {
            locationNames.add(optionLabel(value));
        }
        binding.inventoryLocationInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, locationNames));
        String locationLabel = state.getSelectedLocationId() == null
                ? getString(R.string.inventory_execute_select_location)
                : findOptionLabel(state.getLocations(), state.getSelectedLocationId());
        binding.inventoryLocationInput.setText(locationLabel, false);
        boolean enabled = state.getTask() != null && !state.isReadOnly() && !state.isWriting();
        binding.inventoryWarehouseInput.setEnabled(enabled);
        binding.inventoryLocationInput.setEnabled(enabled);
    }

    private void renderBatch(InventoryExecuteUiState state) {
        String summary;
        if (state.getPreview() != null) {
            summary = "本轮已采 " + state.getReadings().size() + " 个 EPC · 等待确认";
        } else if (isScanning(state)) {
            summary = "正在连续采集 · " + state.getReadings().size() + " 个 EPC";
        } else {
            summary = "本轮采集 " + state.getReadings().size() + " 个 EPC";
        }
        binding.inventoryBatchSummary.setText(summary);
        binding.inventoryBatchDetail.setText("重复读取 " + state.getDuplicateReadCount() + " 次");

        List<PdaInventoryScanDto> exceptions = exceptionRows(state);
        setVisible(binding.inventoryExceptionBar, !exceptions.isEmpty());
        if (!exceptions.isEmpty()) {
            int known = 0;
            int abnormal = 0;
            for (PdaInventoryScanDto row : exceptions) {
                if (row != null && "KNOWN_OUT_OF_SCOPE".equals(row.getMatchType())) {
                    known++;
                } else {
                    abnormal++;
                }
            }
            binding.inventoryExceptionTitle.setText("本轮异常 " + exceptions.size() + " 项");
            binding.inventoryExceptionDetail.setText("范围外资产 " + known
                    + " 项待登记盘盈 · 数据异常 " + abnormal + " 项待处理");
        }
    }

    private void renderExpectedItems(InventoryExecuteUiState state) {
        PdaInventoryTaskDto task = state.getTask();
        int total = task == null ? 0 : (int) task.getTotalCount();
        binding.inventoryExpectedTitle.setText("应盘清单 · 已加载 "
                + state.getExpectedItems().size() + " / " + total);
        itemAdapter.submit(state.getExpectedItems(), state.getItemResultOverrides(),
                state.getPreviewEpcByItemId(), state.getSelectedEpcs(),
                state.isCanSubmit() && !state.isReadOnly() && !state.isWriting());
        setVisible(binding.inventoryExpectedEmpty, !state.isExpectedItemsLoading()
                && task != null && state.getExpectedItems().isEmpty());
        setVisible(binding.inventoryExpectedLoading, state.isExpectedItemsLoading()
                && !state.getExpectedItems().isEmpty());
    }

    private void renderMessages(InventoryExecuteUiState state) {
        setVisible(binding.inventoryMessageText, hasText(state.getInfoMessage()));
        binding.inventoryMessageText.setText(valueOrDash(state.getInfoMessage()));
        setVisible(binding.inventoryErrorText, hasText(state.getErrorMessage()));
        binding.inventoryErrorText.setText(valueOrDash(state.getErrorMessage()));
        setVisible(binding.inventoryReadonlyBanner, state.isReadOnly());
        setVisible(binding.inventoryPermissionHint, !state.isReadOnly()
                && state.getTask() != null && !state.isCanSubmit());
    }

    private void renderActions(InventoryExecuteUiState state) {
        boolean available = state.getTask() != null;
        if (!available) {
            binding.inventoryPrimaryAction.setText(R.string.inventory_execute_loading);
            binding.inventoryPrimaryAction.setEnabled(false);
            setVisible(binding.inventorySecondaryAction, false);
            return;
        }
        setVisible(binding.inventorySecondaryAction, true);
        if (state.isReadOnly()) {
            binding.inventoryPrimaryAction.setText("返回任务");
            binding.inventoryPrimaryAction.setEnabled(true);
            binding.inventorySecondaryAction.setText("盘盈明细");
            binding.inventorySecondaryAction.setEnabled(true);
            return;
        }
        if (state.isWriting()) {
            binding.inventoryPrimaryAction.setText("处理中…");
            binding.inventoryPrimaryAction.setEnabled(false);
            binding.inventorySecondaryAction.setText("请稍候");
            binding.inventorySecondaryAction.setEnabled(false);
            return;
        }
        if (state.getPreview() != null) {
            binding.inventoryPrimaryAction.setText("确认本轮 " + state.getSelectedEpcs().size());
            binding.inventoryPrimaryAction.setEnabled(state.isCanSubmit()
                    && !state.getSelectedEpcs().isEmpty());
            binding.inventorySecondaryAction.setText("继续采集");
            binding.inventorySecondaryAction.setEnabled(true);
            return;
        }
        if (isScanning(state)) {
            binding.inventoryPrimaryAction.setText("停止采集");
            binding.inventoryPrimaryAction.setEnabled(true);
            binding.inventorySecondaryAction.setText("放弃本轮");
            binding.inventorySecondaryAction.setEnabled(true);
            return;
        }
        if (!state.getReadings().isEmpty()) {
            binding.inventoryPrimaryAction.setText("批量预判");
            binding.inventoryPrimaryAction.setEnabled(true);
            binding.inventorySecondaryAction.setText("继续采集");
            binding.inventorySecondaryAction.setEnabled(true);
            return;
        }
        if (state.getTask().getPendingCount() == 0L) {
            binding.inventoryPrimaryAction.setText("最终提交任务");
            binding.inventoryPrimaryAction.setEnabled(state.isCanSubmit());
            binding.inventorySecondaryAction.setText("盘盈明细");
            binding.inventorySecondaryAction.setEnabled(true);
            return;
        }
        binding.inventoryPrimaryAction.setText(state.getLastConfirm() == null
                ? "开始采集" : "下一轮采集");
        binding.inventoryPrimaryAction.setEnabled(true);
        binding.inventorySecondaryAction.setText("全部未盘标记盘亏");
        binding.inventorySecondaryAction.setEnabled(state.isCanSubmit());
    }

    private void renderDialogs(InventoryExecuteUiState state) {
        if (exceptionDialog != null && exceptionDialog.isShowing()) {
            if (state.getPreview() == null) {
                exceptionDialog.dismiss();
            } else {
                previewAdapter.submit(exceptionRows(state), state.getSelectedEpcs(),
                        state.isCanSubmit() && !state.isReadOnly() && !state.isWriting());
            }
        }
        if (surplusDialog != null && surplusDialog.isShowing()) {
            surplusAdapter.submit(state.getSurpluses(), state.isCanSubmit()
                    && !state.isReadOnly() && !state.isWriting());
        }
    }

    private void showExceptionDialog() {
        InventoryExecuteUiState state = currentState();
        if (state == null || state.getPreview() == null) {
            return;
        }
        RecyclerView list = dialogRecyclerView();
        list.setAdapter(previewAdapter);
        previewAdapter.submit(exceptionRows(state), state.getSelectedEpcs(),
                state.isCanSubmit() && !state.isReadOnly() && !state.isWriting());
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("本轮异常")
                .setMessage("范围外已建档资产默认随本轮确认登记盘盈；其他数据异常不会自动写入。")
                .setView(list)
                .setNegativeButton(R.string.common_cancel, null)
                .setNeutralButton(R.string.inventory_execute_identify,
                        (dialog, which) -> showIdentifyDialog());
        if (state.isCanSubmit() && !state.isReadOnly() && !state.getSelectedEpcs().isEmpty()) {
            builder.setPositiveButton("确认本轮", (dialog, which) -> showConfirmDialog());
        }
        exceptionDialog = builder.create();
        exceptionDialog.setOnDismissListener(dialog -> exceptionDialog = null);
        exceptionDialog.show();
    }

    private void showSurplusListDialog() {
        RecyclerView list = dialogRecyclerView();
        list.setAdapter(surplusAdapter);
        InventoryExecuteUiState state = currentState();
        surplusAdapter.submit(state == null ? null : state.getSurpluses(), state != null
                && state.isCanSubmit() && !state.isReadOnly() && !state.isWriting());
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_surplus_title)
                .setView(list)
                .setPositiveButton(R.string.inventory_execute_surplus_confirm, null);
        surplusDialog = builder.create();
        surplusDialog.setOnDismissListener(dialog -> surplusDialog = null);
        surplusDialog.show();
        viewModel.loadSurpluses();
    }

    private RecyclerView dialogRecyclerView() {
        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        int padding = getResources().getDimensionPixelSize(R.dimen.pda_item_spacing);
        list.setPadding(padding, padding, padding, padding);
        list.setClipToPadding(false);
        return list;
    }

    private void showConfirmDialog() {
        InventoryExecuteUiState state = currentState();
        if (state == null || state.getPreview() == null || state.getSelectedEpcs().isEmpty()) {
            return;
        }
        EditText remark = createEditText(getString(R.string.inventory_execute_remark_hint));
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_confirm_title)
                .setMessage("仅确认本轮已选 EPC；范围外资产会登记盘盈。")
                .setView(remark)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm,
                        (dialog, which) -> viewModel.confirmSelected(textOf(remark)))
                .show();
    }

    private void showResumeCollectionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("继续采集")
                .setMessage("保留本轮已采 EPC，继续扫描后会重新预判整批数据。")
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton("继续", (dialog, which) -> viewModel.resumeCollection())
                .show();
    }

    private void showDiscardBatchDialog() {
        new AlertDialog.Builder(this)
                .setTitle("放弃本轮采集？")
                .setMessage("未确认的 EPC 仅保存在当前页面内存中，放弃后无法恢复。")
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton("放弃", (dialog, which) -> viewModel.discardCurrentBatch())
                .show();
    }

    private void showCorrectionDialog(PdaInventoryItemDto item) {
        if (item == null || viewModel == null || viewModel.isReadOnly()) {
            return;
        }
        LinearLayout content = dialogColumn();
        RadioGroup results = new RadioGroup(this);
        RadioButton normal = radio(R.string.inventory_result_normal, "NORMAL");
        RadioButton loss = radio(R.string.inventory_result_loss, "LOSS");
        results.addView(normal);
        results.addView(loss);
        normal.setChecked(true);
        EditText remark = createEditText(getString(R.string.inventory_execute_remark_hint));
        content.addView(results);
        content.addView(remark);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修正 " + valueOrDash(item.getAssetCode()))
                .setView(content)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String result = normal.isChecked() ? "NORMAL" : "LOSS";
                    InventoryExecuteUiState state = currentState();
                    Long warehouseId = "NORMAL".equals(result) && state != null
                            ? state.getSelectedWarehouseId() : null;
                    Long locationId = "NORMAL".equals(result) && state != null
                            ? state.getSelectedLocationId() : null;
                    if ("NORMAL".equals(result) && warehouseId == null) {
                        Toast.makeText(this, "正常盘点结果必须选择实际仓库", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.saveItemResult(item.getItemId(), result, textOf(remark),
                            warehouseId, locationId);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showIdentifyDialog() {
        LinearLayout content = dialogColumn();
        RadioGroup types = new RadioGroup(this);
        RadioButton epc = radio(R.string.inventory_execute_surplus_epc, "EPC");
        RadioButton assetCode = radio(R.string.inventory_execute_surplus_asset_code, "ASSET_CODE");
        types.addView(epc);
        types.addView(assetCode);
        epc.setChecked(true);
        EditText value = createEditText(getString(R.string.inventory_execute_identify_value));
        content.addView(types);
        content.addView(value);
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_identify)
                .setView(content)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> viewModel.identify(
                        epc.isChecked() ? "EPC" : "ASSET_CODE", textOf(value)))
                .show();
    }

    private void showDeleteSurplusDialog(PdaInventorySurplusDto surplus) {
        if (surplus == null || surplus.getSurplusId() == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_surplus_delete)
                .setMessage("确认删除盘盈明细 " + valueOrDash(surplus.getAssetCode()) + "？")
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm,
                        (dialog, which) -> viewModel.removeSurplus(surplus.getSurplusId()))
                .show();
    }

    private void showMarkLossDialog() {
        InventoryExecuteUiState state = currentState();
        PdaInventoryTaskDto task = state == null ? null : state.getTask();
        if (task == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_mark_loss_title)
                .setMessage(getString(R.string.inventory_execute_mark_loss_message,
                        valueOrDash(task.getTaskNo()), task.getPendingCount()))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm,
                        (dialog, which) -> viewModel.markPendingAsLoss(null))
                .show();
    }

    private void showSubmitDialog() {
        InventoryExecuteUiState state = currentState();
        PdaInventoryTaskDto task = state == null ? null : state.getTask();
        if (task == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_submit_title)
                .setMessage(getString(R.string.inventory_execute_submit_message,
                        valueOrDash(task.getTaskNo()), task.getInventoriedCount(), task.getTotalCount(),
                        task.getNormalCount(), task.getSurplusCount()))
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.common_confirm, (dialog, which) -> viewModel.submit())
                .show();
    }

    private void runAfterBatchConfirmation(Runnable action) {
        if (viewModel == null || !viewModel.hasUnconfirmedBatch()) {
            action.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_change_title)
                .setMessage(R.string.inventory_execute_change_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.inventory_execute_change_confirm,
                        (dialog, which) -> {
                            viewModel.discardCurrentBatch();
                            action.run();
                        })
                .show();
    }

    private void handleBack() {
        if (viewModel == null || viewModel.isWriting()) {
            return;
        }
        if (!viewModel.hasUnconfirmedBatch()) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.inventory_execute_leave_title)
                .setMessage(R.string.inventory_execute_leave_message)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.inventory_execute_leave_confirm,
                        (dialog, which) -> finish())
                .show();
    }

    private List<PdaInventoryScanDto> exceptionRows(InventoryExecuteUiState state) {
        List<PdaInventoryScanDto> values = new ArrayList<>();
        if (state == null || state.getPreview() == null || state.getPreview().getRows() == null) {
            return values;
        }
        Set<String> currentEpcs = new HashSet<>();
        for (UhfTagReading reading : state.getReadings()) {
            if (reading != null) {
                currentEpcs.add(reading.getEpc());
            }
        }
        for (PdaInventoryScanDto row : state.getPreview().getRows()) {
            if (row == null || "EXPECTED_ITEM".equals(row.getMatchType())
                    || "UNKNOWN_OBJECT".equals(row.getMatchType())) {
                continue;
            }
            String normalized = normalizeEpc(row.getEpcCode());
            if (normalized != null && currentEpcs.contains(normalized)) {
                values.add(row);
            }
        }
        return values;
    }

    private String normalizeEpc(String value) {
        try {
            return UhfTagReading.normalizeEpc(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isScanning(InventoryExecuteUiState state) {
        return state.getScanState() == UhfScanState.SCANNING
                || state.getScanState() == UhfScanState.PROCESSING;
    }

    private InventoryExecuteUiState currentState() {
        return viewModel == null ? null : viewModel.getUiState().getValue();
    }

    private String formatScope(PdaInventoryTaskDto task) {
        StringBuilder value = new StringBuilder(hasText(task.getWarehouseName())
                ? task.getWarehouseName() : "全部资产");
        if (hasText(task.getLocationName())) {
            value.append(" · ").append(task.getLocationName());
        }
        return value.toString();
    }

    private String optionLabel(PdaMasterDataDto value) {
        if (value == null) {
            return "-";
        }
        if (hasText(value.getCode()) && hasText(value.getName())) {
            return value.getName() + " · " + value.getCode();
        }
        return hasText(value.getName()) ? value.getName() : value.getCode();
    }

    private String findOptionLabel(List<PdaMasterDataDto> values, Long id) {
        if (id != null) {
            for (PdaMasterDataDto value : values) {
                if (value != null && id.equals(value.getId())) {
                    return optionLabel(value);
                }
            }
        }
        return "";
    }

    private EditText createEditText(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinHeight(getResources().getDimensionPixelSize(R.dimen.pda_input_min_height));
        return editText;
    }

    private RadioButton radio(int textResId, String tag) {
        RadioButton button = new RadioButton(this);
        button.setText(textResId);
        button.setTag(tag);
        button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.pda_compact_touch_height));
        return button;
    }

    private LinearLayout dialogColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.pda_card_padding);
        column.setPadding(padding, 0, padding, 0);
        return column;
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? null : editText.getText().toString().trim();
    }

    private String valueOrDash(String value) {
        return hasText(value) ? value : getString(R.string.home_unknown_value);
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
        if (scanKeyDispatcher == null) {
            scanKeyDispatcher = new ScanKeyDispatcher(this,
                    new ScanKeyDispatcher.Listener() {
                        @Override
                        public void onScanKeyPressed() {
                            if (viewModel != null) {
                                // F6 与底部“开始/停止采集”共用同一批量采集状态机。
                                viewModel.onScanKeyPressed();
                            }
                        }
                    });
        }
        scanKeyDispatcher.start();
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
