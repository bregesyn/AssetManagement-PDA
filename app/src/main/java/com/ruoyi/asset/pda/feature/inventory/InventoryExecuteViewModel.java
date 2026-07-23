package com.ruoyi.asset.pda.feature.inventory;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.core.uhf.UhfScanMode;
import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfScanner;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.core.ui.BaseUhfViewModel;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultInventoryRepository;
import com.ruoyi.asset.pda.data.repository.InventoryRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 盘点执行页的唯一业务状态持有者；Activity 只负责把状态映射到控件。 */
public final class InventoryExecuteViewModel extends BaseUhfViewModel {
    public static final int MAX_UNIQUE_EPCS = DefaultInventoryRepository.MAX_BATCH_SIZE;

    private final InventoryRepository inventoryRepository;
    private final CommonRepository commonRepository;
    private final MutableLiveData<InventoryExecuteUiState> uiState;
    private final Long taskId;
    private final String taskNoFromIntent;
    private final boolean canSubmit;
    private final Map<String, UhfTagReading> readingsByEpc = new LinkedHashMap<>();
    private final Set<String> selectedEpcs = new LinkedHashSet<>();
    private final List<PdaMasterDataDto> warehouses = new ArrayList<>();
    private final List<PdaMasterDataDto> locations = new ArrayList<>();
    private final List<PdaInventoryItemDto> expectedItems = new ArrayList<>();
    private final Map<Long, String> itemResultOverrides = new LinkedHashMap<>();
    private final Map<Long, String> previewEpcByItemId = new LinkedHashMap<>();
    private final List<PdaInventorySurplusDto> surpluses = new ArrayList<>();
    private final List<RequestHandle> requests = new ArrayList<>();
    private RequestHandle expectedItemsRequest = RequestHandle.NONE;
    private PdaInventoryTaskDto task;
    private PdaInventoryBatchScanDto preview;
    private PdaInventoryBatchConfirmDto lastConfirm;
    private Long selectedWarehouseId;
    private Long selectedLocationId;
    private int duplicateReadCount;
    private boolean readOnly;
    private boolean writing;
    private boolean initialized;
    private String infoMessage;
    private String errorMessage;
    private Long locationsForWarehouseId;
    private long lastTagPublishedAt;
    private int nextExpectedPage = 1;
    private long expectedItemTotal;
    private boolean expectedItemsLoading;

    public InventoryExecuteViewModel(InventoryRepository inventoryRepository,
            CommonRepository commonRepository, UhfScanner scanner, Long taskId,
            String taskNoFromIntent, boolean canSubmit) {
        super(scanner);
        if (inventoryRepository == null || commonRepository == null || taskId == null) {
            throw new IllegalArgumentException("盘点执行页依赖不能为空");
        }
        this.inventoryRepository = inventoryRepository;
        this.commonRepository = commonRepository;
        this.taskId = taskId;
        this.taskNoFromIntent = taskNoFromIntent;
        this.canSubmit = canSubmit;
        uiState = new MutableLiveData<>(InventoryExecuteUiState.loading(canSubmit));
    }

    public LiveData<InventoryExecuteUiState> getUiState() {
        return uiState;
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        clearMessages();
        track(inventoryRepository.loadTask(taskId, new RepositoryCallback<PdaInventoryTaskDto>() {
            @Override
            public void onSuccess(PdaInventoryTaskDto data) {
                if (data == null) {
                    setError("任务详情响应为空");
                    return;
                }
                applyTask(data);
                publish();
                loadLocationsIfNeeded();
                loadExpectedItems();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                setError(errorMessage(error, "任务详情加载失败"));
            }
        }));
        track(commonRepository.warehouses(new RepositoryCallback<List<PdaMasterDataDto>>() {
            @Override
            public void onSuccess(List<PdaMasterDataDto> data) {
                warehouses.clear();
                if (data != null) {
                    warehouses.addAll(data);
                }
                if (selectedWarehouseId == null && task != null) {
                    selectedWarehouseId = task.getWarehouseId();
                }
                publish();
                loadLocationsIfNeeded();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                setError(errorMessage(error, "仓库主数据加载失败"));
            }
        }));
    }

    public void toggleScan() {
        if (writing || readOnly || preview != null) {
            return;
        }
        if (getCurrentScanState() == UhfScanState.SCANNING
                || getCurrentScanState() == UhfScanState.PROCESSING) {
            stopScanning();
        } else {
            if (selectedWarehouseId == null) {
                setError("请先选择实际仓库");
                return;
            }
            clearMessages();
            startScanning(UhfScanMode.BATCH);
        }
    }

    /** PDA 物理键和屏幕按钮必须共用同一个入口，避免两套扫描状态机互相覆盖。 */
    public void onScanKeyPressed() {
        toggleScan();
    }

    public boolean hasUnconfirmedBatch() {
        return !readingsByEpc.isEmpty();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isWriting() {
        return writing;
    }

    public void changeWarehouse(Long warehouseId) {
        if (readOnly || writing || sameId(selectedWarehouseId, warehouseId)) {
            return;
        }
        stopScanning();
        clearCurrentBatch();
        selectedWarehouseId = warehouseId;
        selectedLocationId = null;
        locations.clear();
        locationsForWarehouseId = null;
        clearMessages();
        publish();
        loadLocationsIfNeeded();
    }

    public void changeLocation(Long locationId) {
        if (readOnly || writing) {
            return;
        }
        if (locationId != null && !containsId(locations, locationId)) {
            setError("所选位置不属于当前仓库");
            return;
        }
        selectedLocationId = locationId;
        clearMessages();
        publish();
    }

    public void discardCurrentBatch() {
        stopScanning();
        clearCurrentBatch();
        clearMessages();
        publish();
    }

    public void precheck() {
        if (!ensureActiveTask() || getCurrentScanState() == UhfScanState.SCANNING
                || getCurrentScanState() == UhfScanState.PROCESSING) {
            return;
        }
        if (selectedWarehouseId == null) {
            setError("请先选择实际仓库");
            return;
        }
        List<String> epcCodes = new ArrayList<>(readingsByEpc.keySet());
        if (epcCodes.isEmpty()) {
            setError("本轮尚未采集 EPC");
            return;
        }
        cancelWritingRequest();
        writing = true;
        preview = null;
        selectedEpcs.clear();
        previewEpcByItemId.clear();
        clearMessages();
        publish();
        track(inventoryRepository.batchScan(taskId, taskNo(), selectedWarehouseId,
                selectedLocationId, epcCodes,
                new RepositoryCallback<PdaInventoryBatchScanDto>() {
                    @Override
                    public void onSuccess(PdaInventoryBatchScanDto data) {
                        writing = false;
                        if (!validatePreview(data, epcCodes)) {
                            setError("预判响应与采集批次不一致，未进入确认");
                            return;
                        }
                        int ignoredUnknownCount = ignoreUnknownEpcs(data.getRows());
                        preview = readingsByEpc.isEmpty() ? null : data;
                        previewEpcByItemId.clear();
                        for (PdaInventoryScanDto row : data.getRows()) {
                            if (isIgnoredUnknownEpc(row)) {
                                continue;
                            }
                            if (isDefaultPreviewSelection(row)) {
                                selectedEpcs.add(normalize(row.getEpcCode()));
                            }
                            addPreviewItemMarker(row);
                        }
                        if (preview == null) {
                            infoMessage = "已忽略 " + ignoredUnknownCount + " 个未知 EPC";
                        } else if (ignoredUnknownCount > 0) {
                            infoMessage = "已忽略 " + ignoredUnknownCount
                                    + " 个未知 EPC；应盘资产与范围外已建档资产已默认选中";
                        } else {
                            infoMessage = "预判完成：应盘资产与范围外已建档资产已默认选中";
                        }
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "批量预判失败"));
                    }
                }));
    }

    public void setPreviewSelection(String epcCode, boolean selected) {
        if (writing || preview == null || !canSelectPreviewRow(epcCode)) {
            return;
        }
        String normalized = normalize(epcCode);
        if (selected) {
            selectedEpcs.add(normalized);
        } else {
            selectedEpcs.remove(normalized);
        }
        clearMessages();
        publish();
    }

    public void confirmSelected(String remark) {
        if (!ensureWritableAction() || preview == null || writing) {
            return;
        }
        if (selectedWarehouseId == null) {
            setError("请先选择实际仓库");
            return;
        }
        List<String> epcCodes = selectedPreviewCodes();
        if (epcCodes.isEmpty()) {
            setError("请至少选择一条可确认记录");
            return;
        }
        stopScanning();
        writing = true;
        clearMessages();
        publish();
        track(inventoryRepository.batchConfirm(taskId, taskNo(), selectedWarehouseId,
                selectedLocationId, epcCodes, remark,
                new RepositoryCallback<PdaInventoryBatchConfirmDto>() {
                    @Override
                    public void onSuccess(PdaInventoryBatchConfirmDto data) {
                        writing = false;
                        if (!validateConfirm(data, epcCodes)) {
                            setError("确认响应与本次提交顺序不一致，已保留本地批次");
                            return;
                        }
                        lastConfirm = data;
                        applyTask(data.getTask());
                        applySuccessfulRows(data, epcCodes);
                        selectedEpcs.clear();
                        if (readingsByEpc.isEmpty()) {
                            preview = null;
                            previewEpcByItemId.clear();
                            duplicateReadCount = 0;
                            infoMessage = "本轮确认完成，可以开始下一轮采集";
                        } else {
                            infoMessage = "本轮部分完成，失败项或未选择项已保留处理入口";
                        }
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "批量确认失败，当前批次已保留"));
                    }
                }));
    }

    public void startNextRound() {
        if (writing || readOnly) {
            return;
        }
        discardCurrentBatch();
        infoMessage = "已清空未确认批次，请开始下一轮采集";
        publish();
    }

    /**
     * 退出预判但保留已采 EPC，下一次开始扫描后可把新旧 EPC 一起重新预判。
     */
    public void resumeCollection() {
        if (writing || readOnly || preview == null) {
            return;
        }
        selectedEpcs.clear();
        preview = null;
        previewEpcByItemId.clear();
        clearMessages();
        infoMessage = "已返回采集，可继续扫描后重新预判本轮 EPC";
        publish();
    }

    public void loadExpectedItems() {
        if (task == null) {
            return;
        }
        if (expectedItemsLoading) {
            return;
        }
        expectedItems.clear();
        itemResultOverrides.clear();
        previewEpcByItemId.clear();
        nextExpectedPage = 1;
        expectedItemTotal = 0L;
        loadExpectedItemPage();
    }

    public void loadMoreExpectedItems() {
        if (task == null || expectedItemsLoading
                || (expectedItemTotal > 0L && expectedItems.size() >= expectedItemTotal)) {
            return;
        }
        loadExpectedItemPage();
    }

    /** 兼容旧调用点；执行页现在始终加载完整应盘清单，而不是单独的未盘面板。 */
    public void loadPendingItems() {
        loadExpectedItems();
    }

    private void loadExpectedItemPage() {
        if (task == null || expectedItemsLoading) {
            return;
        }
        final int requestedPage = nextExpectedPage;
        expectedItemsLoading = true;
        publish();
        expectedItemsRequest = inventoryRepository.loadItems(taskId, null, null, requestedPage,
                DefaultInventoryRepository.TASK_PAGE_SIZE,
                new RepositoryCallback<PdaPageResultDto<PdaInventoryItemDto>>() {
                    @Override
                    public void onSuccess(PdaPageResultDto<PdaInventoryItemDto> data) {
                        expectedItemsRequest = RequestHandle.NONE;
                        expectedItemsLoading = false;
                        if (data != null) {
                            expectedItemTotal = data.getTotal();
                            if (data.getRows() != null) {
                                for (PdaInventoryItemDto item : data.getRows()) {
                                    if (item != null && !containsExpectedItem(item.getItemId())) {
                                        expectedItems.add(item);
                                    }
                                }
                            }
                        }
                        nextExpectedPage = requestedPage + 1;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        expectedItemsRequest = RequestHandle.NONE;
                        expectedItemsLoading = false;
                        setError(errorMessage(error, "应盘明细加载失败"));
                    }
                });
    }

    public void identify(String identifyType, String identifyValue) {
        if (task == null) {
            return;
        }
        track(inventoryRepository.scan(taskId, identifyType, identifyValue,
                new RepositoryCallback<PdaInventoryScanDto>() {
                    @Override
                    public void onSuccess(PdaInventoryScanDto data) {
                        if (data == null) {
                            setError("单条识别响应为空");
                            return;
                        }
                        if ("EXPECTED_ITEM".equals(data.getMatchType()) && data.getItem() != null) {
                            replaceExpectedItem(data.getItem());
                            infoMessage = "识别到应盘资产，可在应盘清单中修正结果";
                        } else {
                            infoMessage = "识别到范围外实物，请通过盘盈登记保存";
                        }
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        setError(errorMessage(error, "单条识别失败"));
                    }
                }));
    }

    public void loadSurpluses() {
        if (task == null) {
            return;
        }
        track(inventoryRepository.loadSurpluses(taskId, 1, 100,
                new RepositoryCallback<PdaPageResultDto<PdaInventorySurplusDto>>() {
                    @Override
                    public void onSuccess(PdaPageResultDto<PdaInventorySurplusDto> data) {
                        surpluses.clear();
                        if (data != null && data.getRows() != null) {
                            surpluses.addAll(data.getRows());
                        }
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        setError(errorMessage(error, "盘盈明细加载失败"));
                    }
                }));
    }

    public void saveItemResult(Long itemId, String result, String remark,
            Long warehouseId, Long locationId) {
        if (!ensureWritableAction()) {
            return;
        }
        writing = true;
        clearMessages();
        publish();
        track(inventoryRepository.saveItemResult(taskId, itemId, result,
                warehouseId, locationId, remark,
                new RepositoryCallback<PdaInventoryItemDto>() {
                    @Override
                    public void onSuccess(PdaInventoryItemDto data) {
                        writing = false;
                        infoMessage = "单条盘点结果已保存";
                        replaceExpectedItem(data);
                        reloadTask();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "单条盘点结果保存失败"));
                    }
                }));
    }

    public void saveSurplus(String identifyMethod, String assetCode, String assetName,
            Long categoryId, String specModel, String brand, String epcCode,
            Long warehouseId, Long locationId, String remark) {
        if (!ensureWritableAction()) {
            return;
        }
        writing = true;
        clearMessages();
        publish();
        track(inventoryRepository.saveSurplus(taskId, identifyMethod, assetCode, assetName,
                categoryId, specModel, brand, epcCode, warehouseId, locationId, remark,
                new RepositoryCallback<PdaInventorySurplusDto>() {
                    @Override
                    public void onSuccess(PdaInventorySurplusDto data) {
                        writing = false;
                        if (data != null) {
                            surpluses.removeIf(item -> item != null
                                    && sameId(item.getSurplusId(), data.getSurplusId()));
                            surpluses.add(data);
                        }
                        infoMessage = "盘盈明细已保存";
                        reloadTask();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "盘盈明细保存失败"));
                    }
                }));
    }

    public void removeSurplus(Long surplusId) {
        if (!ensureWritableAction()) {
            return;
        }
        writing = true;
        clearMessages();
        publish();
        track(inventoryRepository.removeSurplus(taskId, surplusId,
                new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        writing = false;
                        surpluses.removeIf(item -> item != null
                                && sameId(item.getSurplusId(), surplusId));
                        infoMessage = "盘盈明细已删除";
                        reloadTask();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "盘盈明细删除失败"));
                    }
                }));
    }

    public void markPendingAsLoss(String remark) {
        if (!ensureWritableAction()) {
            return;
        }
        writing = true;
        clearMessages();
        publish();
        track(inventoryRepository.markPendingAsLoss(taskId, taskNo(), remark,
                new RepositoryCallback<PdaInventoryBatchLossDto>() {
                    @Override
                    public void onSuccess(PdaInventoryBatchLossDto data) {
                        writing = false;
                        if (data != null) {
                            applyTask(data.getTask());
                        }
                        markLoadedPendingItemsAsLoss();
                        infoMessage = "未盘明细已标记为盘亏";
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        writing = false;
                        setError(errorMessage(error, "批量盘亏失败"));
                    }
                }));
    }

    public void submit() {
        if (!ensureWritableAction() || writing) {
            return;
        }
        writing = true;
        clearMessages();
        publish();
        // 最终提交前重新取任务详情，避免页面上的旧 pendingCount 放行不完整任务。
        track(inventoryRepository.loadTask(taskId, new RepositoryCallback<PdaInventoryTaskDto>() {
            @Override
            public void onSuccess(PdaInventoryTaskDto latest) {
                if (latest == null) {
                    writing = false;
                    setError("提交前任务详情为空，已阻止提交");
                    return;
                }
                applyTask(latest);
                if (latest.getPendingCount() > 0) {
                    writing = false;
                    setError("仍有 " + latest.getPendingCount() + " 条未盘明细，不能提交");
                    return;
                }
                track(inventoryRepository.submit(taskId, taskNo(), null,
                        new RepositoryCallback<PdaInventoryTaskDto>() {
                            @Override
                            public void onSuccess(PdaInventoryTaskDto data) {
                                writing = false;
                                if (data == null) {
                                    setError("提交响应为空，未切换只读状态");
                                    return;
                                }
                                applyTask(data);
                                readOnly = true;
                                clearCurrentBatch();
                                preview = null;
                                selectedEpcs.clear();
                                infoMessage = "任务已提交，当前页面切换为待结果确认只读状态";
                                errorMessage = null;
                                publish();
                            }

                            @Override
                            public void onError(ApiErrorMapper.ApiError error) {
                                writing = false;
                                setError(errorMessage(error, "最终提交失败"));
                            }
                        }));
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                writing = false;
                setError(errorMessage(error, "提交前任务复核失败"));
            }
        }));
    }

    private void reloadTask() {
        track(inventoryRepository.loadTask(taskId, new RepositoryCallback<PdaInventoryTaskDto>() {
            @Override
            public void onSuccess(PdaInventoryTaskDto data) {
                applyTask(data);
                writing = false;
                publish();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                writing = false;
                setError(errorMessage(error, "任务统计刷新失败"));
            }
        }));
    }

    private void applyTask(PdaInventoryTaskDto data) {
        if (data == null) {
            return;
        }
        task = data;
        if (selectedWarehouseId == null) {
            selectedWarehouseId = data.getWarehouseId();
        }
        if ("PENDING_RESULT_CONFIRM".equals(data.getTaskStatus())) {
            readOnly = true;
        }
    }

    private void loadLocationsIfNeeded() {
        if (selectedWarehouseId == null || sameId(locationsForWarehouseId, selectedWarehouseId)) {
            return;
        }
        final Long requestedWarehouseId = selectedWarehouseId;
        locationsForWarehouseId = requestedWarehouseId;
        track(commonRepository.locations(requestedWarehouseId,
                new RepositoryCallback<List<PdaMasterDataDto>>() {
                    @Override
                    public void onSuccess(List<PdaMasterDataDto> data) {
                        if (!sameId(selectedWarehouseId, requestedWarehouseId)) {
                            return;
                        }
                        locations.clear();
                        if (data != null) {
                            locations.addAll(data);
                        }
                        if (selectedLocationId != null && !containsId(locations, selectedLocationId)) {
                            selectedLocationId = null;
                        }
                        // 任务下发位置只是范围信息；实际盘点位置默认“不指定”，绝不替现场按账面位置或 RSSI 猜测。
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (sameId(selectedWarehouseId, requestedWarehouseId)) {
                            setError(errorMessage(error, "位置主数据加载失败"));
                        }
                    }
                }));
    }

    private boolean validatePreview(PdaInventoryBatchScanDto data, List<String> epcCodes) {
        if (data == null || data.getRows() == null || data.getRows().size() != epcCodes.size()
                || data.getTotalCount() != epcCodes.size()) {
            return false;
        }
        Set<Integer> rowNumbers = new HashSet<>();
        int confirmable = 0;
        int unresolved = 0;
        int duplicate = 0;
        int expected = 0;
        int surplus = 0;
        int normal = 0;
        for (int index = 0; index < data.getRows().size(); index++) {
            PdaInventoryScanDto row = data.getRows().get(index);
            if (row == null || row.getRowNumber() == null || row.getConfirmable() == null
                    || row.getEpcCode() == null || !rowNumbers.add(row.getRowNumber())
                    || row.getRowNumber() != index + 1) {
                return false;
            }
            String responseEpc = normalize(row.getEpcCode());
            if (responseEpc == null || !epcCodes.get(index).equals(responseEpc)) {
                return false;
            }
            if ("DUPLICATE_INPUT".equals(row.getReasonCode())) {
                duplicate++;
            }
            if (!Boolean.TRUE.equals(row.getConfirmable())) {
                unresolved++;
                continue;
            }
            confirmable++;
            if ("EXPECTED_ITEM".equals(row.getMatchType())) {
                expected++;
                if (!DefaultInventoryRepository.RESULT_NORMAL.equals(row.getProposedResult())) {
                    return false;
                }
                normal++;
            } else {
                surplus++;
            }
        }
        return data.getConfirmableCount() == confirmable
                && data.getUnresolvedCount() == unresolved
                && data.getDuplicateCount() == duplicate
                && data.getExpectedCount() == expected
                && data.getSurplusCount() == surplus
                && data.getNormalCount() == normal;
    }

    private boolean validateConfirm(PdaInventoryBatchConfirmDto data, List<String> epcCodes) {
        if (data == null || data.getRows() == null || data.getRows().size() != epcCodes.size()
                || data.getTotalCount() != epcCodes.size()) {
            return false;
        }
        Set<Integer> rowNumbers = new HashSet<>();
        for (int index = 0; index < data.getRows().size(); index++) {
            PdaInventoryScanDto row = data.getRows().get(index);
            if (row == null || row.getRowNumber() == null || row.getSuccess() == null
                    || !rowNumbers.add(row.getRowNumber())
                    || row.getRowNumber() != index + 1
                    || !epcCodes.get(index).equals(normalize(row.getEpcCode()))) {
                return false;
            }
        }
        return data.getSuccessCount() >= 0
                && data.getFailureCount() >= 0
                && data.getSuccessCount() + data.getFailureCount() <= data.getTotalCount();
    }

    private void applySuccessfulRows(PdaInventoryBatchConfirmDto data, List<String> submitted) {
        for (int index = 0; index < data.getRows().size(); index++) {
            PdaInventoryScanDto row = data.getRows().get(index);
            if (Boolean.TRUE.equals(row.getSuccess())) {
                readingsByEpc.remove(submitted.get(index));
                if ("EXPECTED_ITEM".equals(row.getMatchType()) && row.getItem() != null
                        && row.getItem().getItemId() != null) {
                    // 只有服务端确认成功后才将清单行从蓝色待确认切换为绿色正常。
                    itemResultOverrides.put(row.getItem().getItemId(),
                            DefaultInventoryRepository.RESULT_NORMAL);
                    previewEpcByItemId.remove(row.getItem().getItemId());
                }
            }
        }
    }

    /**
     * 盘点只核对已建档资产；未知 EPC 既不形成盘盈也不进入待确认批次。
     * 不跨轮缓存忽略结果，后续完成建档后重新扫描仍可被正常识别。
     */
    private int ignoreUnknownEpcs(List<PdaInventoryScanDto> rows) {
        int ignored = 0;
        if (rows == null) {
            return ignored;
        }
        for (PdaInventoryScanDto row : rows) {
            if (!isIgnoredUnknownEpc(row)) {
                continue;
            }
            String epc = normalize(row.getEpcCode());
            if (epc != null && readingsByEpc.remove(epc) != null) {
                ignored++;
            }
        }
        if (ignored > 0) {
            refreshDuplicateReadCount();
        }
        return ignored;
    }

    private boolean isIgnoredUnknownEpc(PdaInventoryScanDto row) {
        return row != null && "UNKNOWN_OBJECT".equals(row.getMatchType())
                && !Boolean.TRUE.equals(row.getConfirmable());
    }

    private void refreshDuplicateReadCount() {
        duplicateReadCount = 0;
        for (UhfTagReading reading : readingsByEpc.values()) {
            if (reading != null) {
                duplicateReadCount += reading.getReadCount() - 1;
            }
        }
    }

    private boolean isDefaultPreviewSelection(PdaInventoryScanDto row) {
        return row != null && Boolean.TRUE.equals(row.getConfirmable())
                && ("EXPECTED_ITEM".equals(row.getMatchType())
                || "KNOWN_OUT_OF_SCOPE".equals(row.getMatchType()));
    }

    private void addPreviewItemMarker(PdaInventoryScanDto row) {
        if (row == null || !"EXPECTED_ITEM".equals(row.getMatchType())
                || row.getItem() == null || row.getItem().getItemId() == null) {
            return;
        }
        String epc = normalize(row.getEpcCode());
        if (epc != null) {
            previewEpcByItemId.put(row.getItem().getItemId(), epc);
        }
    }

    private boolean canSelectPreviewRow(String epcCode) {
        String normalized = normalize(epcCode);
        if (normalized == null || preview == null || preview.getRows() == null) {
            return false;
        }
        for (PdaInventoryScanDto row : preview.getRows()) {
            if (row != null && normalized.equals(normalize(row.getEpcCode()))) {
                return Boolean.TRUE.equals(row.getConfirmable());
            }
        }
        return false;
    }

    private List<String> selectedPreviewCodes() {
        List<String> values = new ArrayList<>();
        if (preview == null || preview.getRows() == null) {
            return values;
        }
        for (PdaInventoryScanDto row : preview.getRows()) {
            String epc = normalize(row == null ? null : row.getEpcCode());
            if (epc != null && selectedEpcs.contains(epc)) {
                values.add(epc);
            }
        }
        return values;
    }

    private void clearCurrentBatch() {
        readingsByEpc.clear();
        duplicateReadCount = 0;
        selectedEpcs.clear();
        preview = null;
        previewEpcByItemId.clear();
    }

    private boolean ensureWritableAction() {
        if (!canSubmit) {
            setError("当前账号没有盘点提交权限，只能查看和预判");
            return false;
        }
        if (readOnly || task == null) {
            setError("当前任务已进入只读状态");
            return false;
        }
        return true;
    }

    /** 预判为只读接口，查看权限用户也可以核对本轮扫描结果。 */
    private boolean ensureActiveTask() {
        if (readOnly || task == null) {
            setError("当前任务已进入只读状态");
            return false;
        }
        return true;
    }

    private void onReadOnlyTask() {
        readOnly = true;
    }

    private void cancelWritingRequest() {
        // 预判/确认互斥，避免迟到回调把新一轮状态覆盖掉。
        for (RequestHandle request : requests) {
            request.cancel();
        }
        requests.clear();
    }

    private RequestHandle track(RequestHandle request) {
        if (request != null) {
            requests.add(request);
        }
        return request;
    }

    private String taskNo() {
        if (task != null && task.getTaskNo() != null && !task.getTaskNo().trim().isEmpty()) {
            return task.getTaskNo().trim();
        }
        return taskNoFromIntent == null ? "" : taskNoFromIntent.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UhfTagReading.normalizeEpc(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void replaceExpectedItem(PdaInventoryItemDto data) {
        if (data == null || data.getItemId() == null) {
            return;
        }
        for (int index = 0; index < expectedItems.size(); index++) {
            PdaInventoryItemDto item = expectedItems.get(index);
            if (item != null && sameId(item.getItemId(), data.getItemId())) {
                expectedItems.set(index, data);
                itemResultOverrides.remove(data.getItemId());
                previewEpcByItemId.remove(data.getItemId());
                return;
            }
        }
    }

    private void markLoadedPendingItemsAsLoss() {
        for (PdaInventoryItemDto item : expectedItems) {
            if (item == null || item.getItemId() == null) {
                continue;
            }
            String result = itemResultOverrides.containsKey(item.getItemId())
                    ? itemResultOverrides.get(item.getItemId()) : item.getInventoryResult();
            if (result == null || result.trim().isEmpty()) {
                itemResultOverrides.put(item.getItemId(), DefaultInventoryRepository.RESULT_LOSS);
                previewEpcByItemId.remove(item.getItemId());
            }
        }
    }

    private boolean containsExpectedItem(Long itemId) {
        if (itemId == null) {
            return false;
        }
        for (PdaInventoryItemDto item : expectedItems) {
            if (item != null && sameId(item.getItemId(), itemId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsId(List<PdaMasterDataDto> values, Long id) {
        if (id == null) {
            return false;
        }
        for (PdaMasterDataDto value : values) {
            if (value != null && sameId(value.getId(), id)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameId(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private String errorMessage(ApiErrorMapper.ApiError error, String fallback) {
        return error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? fallback : error.getMessage();
    }

    private void setError(String message) {
        errorMessage = message;
        infoMessage = null;
        publish();
    }

    private void clearMessages() {
        infoMessage = null;
        errorMessage = null;
    }

    private void publish() {
        InventoryExecuteUiState.Mode mode;
        if (task == null) {
            mode = InventoryExecuteUiState.Mode.LOADING;
        } else if (readOnly) {
            mode = InventoryExecuteUiState.Mode.READONLY;
        } else if (preview != null) {
            mode = InventoryExecuteUiState.Mode.PREVIEW;
        } else {
            mode = InventoryExecuteUiState.Mode.READY;
        }
        uiState.setValue(new InventoryExecuteUiState(mode, task, getCurrentScanState(), writing,
                canSubmit, readOnly, new ArrayList<>(readingsByEpc.values()), duplicateReadCount,
                latestReading(), preview, selectedEpcs, lastConfirm, warehouses, locations,
                selectedWarehouseId, selectedLocationId, expectedItems, itemResultOverrides,
                previewEpcByItemId, expectedItemsLoading,
                expectedItemTotal > expectedItems.size(), surpluses, infoMessage, errorMessage));
    }

    private UhfTagReading latestReading() {
        UhfTagReading latest = null;
        for (UhfTagReading value : readingsByEpc.values()) {
            latest = value;
        }
        return latest;
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        publish();
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        if (reading == null || writing || readOnly || preview != null) {
            return;
        }
        UhfTagReading previous = readingsByEpc.get(reading.getEpc());
        if (previous == null && readingsByEpc.size() >= MAX_UNIQUE_EPCS) {
            stopScanning();
            setError("已达到单批 500 条 EPC 上限，请先提交当前批次");
            return;
        }
        if (previous == null) {
            readingsByEpc.put(reading.getEpc(), reading);
        } else {
            duplicateReadCount++;
            readingsByEpc.put(reading.getEpc(), previous.next(
                    reading.getRssi(), reading.getLastSeenAt()));
        }
        errorMessage = null;
        // 使用 JVM/Android 都可用的单调时钟，只用于限制清单刷新频率，不参与业务时间。
        long now = System.nanoTime() / 1_000_000L;
        if (lastTagPublishedAt == 0L || now - lastTagPublishedAt >= 150L) {
            lastTagPublishedAt = now;
            publish();
        }
    }

    @Override
    protected void onScanError(String message) {
        setError(message == null || message.trim().isEmpty() ? "UHF 采集失败，请重试" : message);
    }

    @Override
    protected void onViewModelCleared() {
        cancelWritingRequest();
        expectedItemsRequest.cancel();
        expectedItemsRequest = RequestHandle.NONE;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final InventoryRepository inventoryRepository;
        private final CommonRepository commonRepository;
        private final UhfScanner scanner;
        private final Long taskId;
        private final String taskNo;
        private final boolean canSubmit;

        public Factory(InventoryRepository inventoryRepository, CommonRepository commonRepository,
                UhfScanner scanner, Long taskId, String taskNo, boolean canSubmit) {
            this.inventoryRepository = inventoryRepository;
            this.commonRepository = commonRepository;
            this.scanner = scanner;
            this.taskId = taskId;
            this.taskNo = taskNo;
            this.canSubmit = canSubmit;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(InventoryExecuteViewModel.class)) {
                throw new IllegalArgumentException("不支持的盘点执行 ViewModel 类型");
            }
            return (T) new InventoryExecuteViewModel(inventoryRepository, commonRepository,
                    scanner, taskId, taskNo, canSubmit);
        }
    }
}
