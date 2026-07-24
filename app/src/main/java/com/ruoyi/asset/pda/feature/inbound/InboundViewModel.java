package com.ruoyi.asset.pda.feature.inbound;

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
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInboundEligibilityDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultInboundRepository;
import com.ruoyi.asset.pda.data.repository.InboundRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 入库作业的唯一状态持有者；扫描、预检和事务确认保持三个明确阶段。 */
public final class InboundViewModel extends BaseUhfViewModel {
    private final InboundRepository inboundRepository;
    private final CommonRepository commonRepository;
    private final boolean canConfirm;
    private final MutableLiveData<InboundUiState> uiState = new MutableLiveData<>();
    private final List<PdaMasterDataDto> warehouses = new ArrayList<>();
    private final List<PdaMasterDataDto> locations = new ArrayList<>();
    private final Map<String, UhfTagReading> readingsByEpc = new LinkedHashMap<>();
    private final Map<Long, InboundAssetItem> assetsById = new LinkedHashMap<>();
    private final Set<String> acceptedEpcs = new LinkedHashSet<>();
    private final Map<String, InboundIssueItem> issuesByKey = new LinkedHashMap<>();

    private RequestHandle bootstrapRequest = RequestHandle.NONE;
    private RequestHandle warehousesRequest = RequestHandle.NONE;
    private RequestHandle locationRequest = RequestHandle.NONE;
    private RequestHandle operationRequest = RequestHandle.NONE;
    private boolean initialized;
    private boolean bootstrapLoaded;
    private boolean warehousesLoaded;
    private boolean initialFailed;
    private int initializationVersion;
    private int locationVersion;
    private int operationVersion;
    private InboundUiState.Operation operation = InboundUiState.Operation.NONE;
    private Long selectedWarehouseId;
    private Long selectedLocationId;
    private String operatorName;
    private String serverTime;
    private int duplicateReadCount;
    private String latestEpc;
    private String infoMessage;
    private String errorMessage;
    private PdaInboundBatchConfirmDto lastConfirmation;
    private int assetCodeClearVersion;
    private int batchResetVersion;
    private long lastTagPublishedAt;

    public InboundViewModel(InboundRepository inboundRepository,
            CommonRepository commonRepository, UhfScanner scanner,
            boolean canConfirm) {
        super(scanner);
        if (inboundRepository == null || commonRepository == null) {
            throw new IllegalArgumentException("入库 Repository 不能为空");
        }
        this.inboundRepository = inboundRepository;
        this.commonRepository = commonRepository;
        this.canConfirm = canConfirm;
        publish();
    }

    public LiveData<InboundUiState> getUiState() {
        return uiState;
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        startInitialization();
    }

    public void retryInitialization() {
        if (operation != InboundUiState.Operation.NONE) {
            return;
        }
        if (initialFailed || warehouses.isEmpty()) {
            startInitialization();
        } else if (selectedWarehouseId != null && locations.isEmpty()) {
            clearMessages();
            loadLocations(selectedWarehouseId);
        }
    }

    private void startInitialization() {
        cancelInitialRequests();
        int version = ++initializationVersion;
        bootstrapLoaded = false;
        warehousesLoaded = false;
        initialFailed = false;
        warehouses.clear();
        locations.clear();
        selectedWarehouseId = null;
        selectedLocationId = null;
        clearMessages();
        publish();

        RequestHandle bootstrap = commonRepository.bootstrap(
                new RepositoryCallback<PdaBootstrapDto>() {
                    @Override
                    public void onSuccess(PdaBootstrapDto data) {
                        if (version != initializationVersion || initialFailed) {
                            return;
                        }
                        PdaUserDto user = data == null ? null : data.getCurrentUser();
                        if (user == null || user.getUserId() == null
                                || !hasText(user.getLoginName())) {
                            failInitialization(version, "当前用户信息不完整，请重新登录");
                            return;
                        }
                        operatorName = hasText(user.getUserName())
                                ? user.getUserName().trim() : user.getLoginName().trim();
                        serverTime = data.getServerTime();
                        bootstrapLoaded = true;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        failInitialization(version,
                                messageOf(error, "当前用户信息加载失败"));
                    }
                });
        if (version == initializationVersion && !bootstrapLoaded && !initialFailed) {
            bootstrapRequest = bootstrap;
        }

        RequestHandle warehouseLoad = commonRepository.warehouses(
                new RepositoryCallback<List<PdaMasterDataDto>>() {
                    @Override
                    public void onSuccess(List<PdaMasterDataDto> data) {
                        if (version != initializationVersion || initialFailed) {
                            return;
                        }
                        warehouses.clear();
                        if (data != null) {
                            for (PdaMasterDataDto value : data) {
                                if (validMasterData(value)) {
                                    warehouses.add(value);
                                }
                            }
                        }
                        warehousesLoaded = true;
                        if (warehouses.isEmpty()) {
                            errorMessage = "当前没有可用的入库仓库";
                        }
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        failInitialization(version,
                                messageOf(error, "仓库主数据加载失败"));
                    }
                });
        if (version == initializationVersion && !warehousesLoaded && !initialFailed) {
            warehousesRequest = warehouseLoad;
        }
    }

    private void failInitialization(int version, String message) {
        if (version != initializationVersion) {
            return;
        }
        initialFailed = true;
        errorMessage = message;
        cancelInitialRequests();
        publish();
    }

    public void changeWarehouse(Long warehouseId) {
        if (!readyForInput() || operation != InboundUiState.Operation.NONE
                || sameId(selectedWarehouseId, warehouseId)
                || !containsId(warehouses, warehouseId)) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        selectedWarehouseId = warehouseId;
        selectedLocationId = null;
        locations.clear();
        clearMessages();
        loadLocations(warehouseId);
    }

    private void loadLocations(Long warehouseId) {
        cancelLocationRequest();
        int version = ++locationVersion;
        operation = InboundUiState.Operation.LOCATION;
        publish();
        RequestHandle request = commonRepository.locations(warehouseId,
                new RepositoryCallback<List<PdaMasterDataDto>>() {
                    @Override
                    public void onSuccess(List<PdaMasterDataDto> data) {
                        if (version != locationVersion
                                || !sameId(selectedWarehouseId, warehouseId)) {
                            return;
                        }
                        locations.clear();
                        if (data != null) {
                            for (PdaMasterDataDto value : data) {
                                if (validMasterData(value)
                                        && sameId(value.getParentId(), warehouseId)) {
                                    locations.add(value);
                                }
                            }
                        }
                        operation = InboundUiState.Operation.NONE;
                        if (locations.isEmpty()) {
                            errorMessage = "所选仓库没有可用的入库位置";
                        }
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == locationVersion) {
                            operation = InboundUiState.Operation.NONE;
                            errorMessage = messageOf(error, "位置主数据加载失败");
                            publish();
                        }
                    }
                });
        if (version == locationVersion
                && operation == InboundUiState.Operation.LOCATION) {
            locationRequest = request;
        }
    }

    public void changeLocation(Long locationId) {
        if (!readyForInput() || operation != InboundUiState.Operation.NONE
                || sameId(selectedLocationId, locationId)
                || !containsId(locations, locationId)) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        selectedLocationId = locationId;
        clearMessages();
        publish();
    }

    /** F6 和屏幕按钮共享此入口，避免形成第二套扫描状态机。 */
    public void toggleScan() {
        if (!readyForInput() || operation != InboundUiState.Operation.NONE) {
            return;
        }
        if (isScanning()) {
            stopScanning();
            precheckReadings();
            return;
        }
        if (!readingsByEpc.isEmpty()) {
            precheckReadings();
            return;
        }
        if (!destinationSelected()) {
            setError("请先选择入库仓库和位置");
            return;
        }
        if (assetsById.size() >= DefaultInboundRepository.MAX_BATCH_SIZE) {
            setError("当前批次已达到 100 件上限");
            return;
        }
        lastConfirmation = null;
        clearMessages();
        startScanning(UhfScanMode.BATCH);
    }

    public void onScanKeyPressed() {
        toggleScan();
    }

    private void precheckReadings() {
        if (readingsByEpc.isEmpty()) {
            infoMessage = "本轮未读取到 RFID 标签";
            publish();
            return;
        }
        List<String> submitted = new ArrayList<>(readingsByEpc.keySet());
        cancelOperationRequest();
        int version = ++operationVersion;
        operation = InboundUiState.Operation.PRECHECK;
        clearMessages();
        publish();
        RequestHandle request = inboundRepository.batchCheck(submitted,
                new RepositoryCallback<PdaInboundBatchCheckDto>() {
                    @Override
                    public void onSuccess(PdaInboundBatchCheckDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        if (!validBatchCheck(data, submitted)) {
                            operation = InboundUiState.Operation.NONE;
                            errorMessage = "预检响应与本轮 EPC 不一致，请重试";
                            publish();
                            return;
                        }
                        applyBatchCheck(data);
                        readingsByEpc.clear();
                        latestEpc = null;
                        operation = InboundUiState.Operation.NONE;
                        infoMessage = "预检完成：加入 " + data.getEligibleCount()
                                + " 件，异常 " + (data.getTotalCount()
                                - data.getEligibleCount()) + " 件";
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = InboundUiState.Operation.NONE;
                            errorMessage = messageOf(error,
                                    "批量预检失败，已保留本轮 EPC");
                            publish();
                        }
                    }
                });
        if (version == operationVersion
                && operation == InboundUiState.Operation.PRECHECK) {
            operationRequest = request;
        }
    }

    public void addByAssetCode(String assetCode) {
        if (!readyForInput() || operation != InboundUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!destinationSelected()) {
            setError("请先选择入库仓库和位置");
            return;
        }
        String checkedCode = trim(assetCode);
        if (checkedCode == null) {
            setError("请输入资产编码");
            return;
        }
        if (checkedCode.length() > DefaultInboundRepository.MAX_ASSET_CODE_LENGTH) {
            setError("资产编码长度不能超过 64 个字符");
            return;
        }
        if (assetsById.size() >= DefaultInboundRepository.MAX_BATCH_SIZE) {
            setError("当前批次已达到 100 件上限");
            return;
        }
        cancelOperationRequest();
        int version = ++operationVersion;
        operation = InboundUiState.Operation.ASSET_QUERY;
        lastConfirmation = null;
        clearMessages();
        publish();
        RequestHandle request = inboundRepository.queryByAssetCode(checkedCode,
                new RepositoryCallback<PdaInboundEligibilityDto>() {
                    @Override
                    public void onSuccess(PdaInboundEligibilityDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        operation = InboundUiState.Operation.NONE;
                        if (!validEligibility(data)) {
                            errorMessage = "资产资格响应缺少必要信息";
                            publish();
                            return;
                        }
                        if (!data.isEligible()) {
                            errorMessage = hasText(data.getIneligibleReason())
                                    ? data.getIneligibleReason()
                                    : "该资产当前不允许入库";
                            publish();
                            return;
                        }
                        if (assetsById.containsKey(data.getAssetId())) {
                            infoMessage = "资产已在当前入库清单中";
                        } else {
                            assetsById.put(data.getAssetId(),
                                    InboundAssetItem.fromEligibility(data));
                            infoMessage = "已加入 " + data.getAssetCode();
                        }
                        assetCodeClearVersion++;
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = InboundUiState.Operation.NONE;
                            errorMessage = messageOf(error, "资产查询失败");
                            publish();
                        }
                    }
                });
        if (version == operationVersion
                && operation == InboundUiState.Operation.ASSET_QUERY) {
            operationRequest = request;
        }
    }

    public void removeAsset(Long assetId) {
        if (assetId == null || operation != InboundUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        InboundAssetItem removed = assetsById.remove(assetId);
        if (removed == null) {
            return;
        }
        if (hasText(removed.getEpcCode())) {
            acceptedEpcs.remove(removed.getEpcCode());
        }
        lastConfirmation = null;
        infoMessage = "已从本批次移除 " + removed.getAssetCode();
        errorMessage = null;
        publish();
    }

    public void clearBatch() {
        if (operation != InboundUiState.Operation.NONE) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        infoMessage = "当前入库批次已清空";
        errorMessage = null;
        publish();
    }

    public void confirm(String remark) {
        if (!readyForInput() || operation != InboundUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!canConfirm) {
            setError("当前账号没有入库确认权限");
            return;
        }
        if (!destinationSelected()) {
            setError("请先选择入库仓库和位置");
            return;
        }
        if (assetsById.isEmpty()) {
            setError("请至少加入一件可入库资产");
            return;
        }
        String checkedRemark = trim(remark);
        if (checkedRemark != null
                && checkedRemark.length() > DefaultInboundRepository.MAX_REMARK_LENGTH) {
            setError("备注长度不能超过 500 个字符");
            return;
        }
        List<Long> submittedIds = new ArrayList<>(assetsById.keySet());
        cancelOperationRequest();
        int version = ++operationVersion;
        operation = InboundUiState.Operation.CONFIRM;
        clearMessages();
        publish();
        RequestHandle request = inboundRepository.batchConfirm(selectedWarehouseId,
                selectedLocationId, submittedIds, checkedRemark,
                new RepositoryCallback<PdaInboundBatchConfirmDto>() {
                    @Override
                    public void onSuccess(PdaInboundBatchConfirmDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        if (!validConfirmation(data, submittedIds)) {
                            operation = InboundUiState.Operation.NONE;
                            errorMessage = "确认响应与提交资产不一致，请到后台核对入库单";
                            publish();
                            return;
                        }
                        clearWorkingBatch(false);
                        operation = InboundUiState.Operation.NONE;
                        lastConfirmation = data;
                        if (hasText(data.getInboundUserName())) {
                            operatorName = data.getInboundUserName();
                        }
                        if (hasText(data.getInboundTime())) {
                            serverTime = data.getInboundTime();
                        }
                        infoMessage = "入库成功：" + data.getInboundNo();
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = InboundUiState.Operation.NONE;
                            String base = messageOf(error, "入库确认失败");
                            if (error != null && (error.getKind() == ApiErrorMapper.Kind.NETWORK
                                    || error.getKind() == ApiErrorMapper.Kind.TIMEOUT
                                    || error.getKind() == ApiErrorMapper.Kind.PROTOCOL
                                    || error.getKind() == ApiErrorMapper.Kind.SYSTEM)) {
                                base += "；结果可能未知，请先到后台核对入库单，勿直接重复提交";
                            }
                            errorMessage = base;
                            publish();
                        }
                    }
                });
        if (version == operationVersion
                && operation == InboundUiState.Operation.CONFIRM) {
            operationRequest = request;
        }
    }

    public boolean hasPendingWork() {
        return !assetsById.isEmpty() || !readingsByEpc.isEmpty()
                || !issuesByKey.isEmpty();
    }

    private void applyBatchCheck(PdaInboundBatchCheckDto data) {
        for (PdaInboundBatchCheckDto.Row row : data.getRows()) {
            if ("ELIGIBLE".equals(row.getStatus())) {
                removeIssuesForEpc(row.getEpcCode());
                acceptedEpcs.add(row.getEpcCode());
                if (assetsById.containsKey(row.getAssetId())) {
                    duplicateReadCount++;
                } else {
                    assetsById.put(row.getAssetId(),
                            InboundAssetItem.fromBatchRow(row));
                }
            } else {
                issuesByKey.put(issueKey(row), new InboundIssueItem(row));
            }
        }
        lastConfirmation = null;
    }

    private boolean validBatchCheck(PdaInboundBatchCheckDto data,
            List<String> submitted) {
        if (data == null || data.getRows() == null
                || data.getTotalCount() != submitted.size()
                || data.getRows().size() != submitted.size()
                || data.getEligibleCount() < 0 || data.getIneligibleCount() < 0
                || data.getUnknownCount() < 0
                || data.getEligibleCount() + data.getIneligibleCount()
                + data.getUnknownCount() != data.getTotalCount()) {
            return false;
        }
        Set<Long> eligibleIds = new HashSet<>();
        for (int index = 0; index < submitted.size(); index++) {
            PdaInboundBatchCheckDto.Row row = data.getRows().get(index);
            if (row == null || !submitted.get(index).equals(row.getEpcCode())
                    || (!"ELIGIBLE".equals(row.getStatus())
                    && !"INELIGIBLE".equals(row.getStatus())
                    && !"UNKNOWN".equals(row.getStatus()))) {
                return false;
            }
            if ("ELIGIBLE".equals(row.getStatus())
                    && (!validBatchAsset(row) || !eligibleIds.add(row.getAssetId()))) {
                return false;
            }
        }
        return true;
    }

    private boolean validBatchAsset(PdaInboundBatchCheckDto.Row row) {
        return row.getAssetId() != null && row.getAssetId() > 0L
                && hasText(row.getAssetCode()) && hasText(row.getAssetName());
    }

    private boolean validEligibility(PdaInboundEligibilityDto data) {
        return data != null && data.getAssetId() != null && data.getAssetId() > 0L
                && hasText(data.getAssetCode()) && hasText(data.getAssetName());
    }

    private boolean validConfirmation(PdaInboundBatchConfirmDto data,
            List<Long> submittedIds) {
        if (data == null || data.getOrderId() == null || data.getOrderId() < 1L
                || !hasText(data.getInboundNo()) || !hasText(data.getInboundTime())
                || data.getTotalCount() != submittedIds.size()
                || data.getSuccessCount() != submittedIds.size()
                || data.getRows() == null
                || data.getRows().size() != submittedIds.size()) {
            return false;
        }
        Set<Long> expected = new HashSet<>(submittedIds);
        Set<Long> returned = new HashSet<>();
        for (PdaInboundBatchConfirmDto.Row row : data.getRows()) {
            if (row == null || row.getAssetId() == null
                    || !"SUCCESS".equals(row.getStatus())
                    || !expected.contains(row.getAssetId())
                    || !returned.add(row.getAssetId())) {
                return false;
            }
        }
        return returned.equals(expected);
    }

    private void clearWorkingBatch(boolean clearConfirmation) {
        readingsByEpc.clear();
        assetsById.clear();
        acceptedEpcs.clear();
        issuesByKey.clear();
        duplicateReadCount = 0;
        latestEpc = null;
        lastTagPublishedAt = 0L;
        batchResetVersion++;
        if (clearConfirmation) {
            lastConfirmation = null;
        }
    }

    private boolean destinationSelected() {
        return selectedWarehouseId != null && selectedLocationId != null;
    }

    private boolean readyForInput() {
        return !initialFailed && bootstrapLoaded && warehousesLoaded;
    }

    private boolean isScanning() {
        UhfScanState state = getCurrentScanState();
        return state == UhfScanState.SCANNING || state == UhfScanState.PROCESSING;
    }

    private boolean validMasterData(PdaMasterDataDto value) {
        return value != null && value.getId() != null && value.getId() > 0L
                && hasText(value.getName());
    }

    private boolean containsId(List<PdaMasterDataDto> values, Long id) {
        if (id == null) {
            return false;
        }
        for (PdaMasterDataDto value : values) {
            if (value != null && id.equals(value.getId())) {
                return true;
            }
        }
        return false;
    }

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
    }

    private String issueKey(PdaInboundBatchCheckDto.Row row) {
        return String.valueOf(row.getEpcCode()) + "|" + String.valueOf(row.getStatus());
    }

    private void removeIssuesForEpc(String epcCode) {
        if (!hasText(epcCode)) {
            return;
        }
        String prefix = epcCode + "|";
        issuesByKey.keySet().removeIf(key -> key.startsWith(prefix));
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

    private String messageOf(ApiErrorMapper.ApiError error, String fallback) {
        return error != null && hasText(error.getMessage())
                ? error.getMessage() : fallback;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void publish() {
        boolean initialLoading = !initialFailed
                && (!bootstrapLoaded || !warehousesLoaded);
        boolean initialReady = !initialFailed && bootstrapLoaded && warehousesLoaded;
        uiState.setValue(new InboundUiState(initialLoading, initialReady,
                initialFailed, canConfirm, operation,
                getCurrentScanState(), operatorName, serverTime, warehouses, locations,
                selectedWarehouseId, selectedLocationId,
                new ArrayList<>(assetsById.values()),
                new ArrayList<>(issuesByKey.values()), readingsByEpc.size(),
                duplicateReadCount, latestEpc, infoMessage, errorMessage,
                lastConfirmation, assetCodeClearVersion, batchResetVersion));
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        publish();
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        if (reading == null || operation != InboundUiState.Operation.NONE) {
            return;
        }
        String epc = reading.getEpc();
        latestEpc = epc;
        if (acceptedEpcs.contains(epc)) {
            duplicateReadCount++;
        } else if (readingsByEpc.containsKey(epc)) {
            duplicateReadCount++;
            readingsByEpc.put(epc, reading);
        } else {
            readingsByEpc.put(epc, reading);
        }
        int remaining = DefaultInboundRepository.MAX_BATCH_SIZE - assetsById.size();
        if (readingsByEpc.size() >= remaining) {
            stopScanning();
            publish();
            precheckReadings();
            return;
        }
        // 高频读数只合并界面刷新，完整 EPC 集合仍逐条保存在内存中。
        long now = System.nanoTime() / 1_000_000L;
        if (lastTagPublishedAt == 0L || now - lastTagPublishedAt >= 150L) {
            lastTagPublishedAt = now;
            publish();
        }
    }

    @Override
    protected void onScanError(String message) {
        setError(hasText(message) ? message : "UHF 扫描失败，请重试");
    }

    private void cancelInitialRequests() {
        bootstrapRequest.cancel();
        warehousesRequest.cancel();
        bootstrapRequest = RequestHandle.NONE;
        warehousesRequest = RequestHandle.NONE;
    }

    private void cancelLocationRequest() {
        locationRequest.cancel();
        locationRequest = RequestHandle.NONE;
    }

    private void cancelOperationRequest() {
        operationRequest.cancel();
        operationRequest = RequestHandle.NONE;
    }

    @Override
    protected void onViewModelCleared() {
        initializationVersion++;
        locationVersion++;
        operationVersion++;
        cancelInitialRequests();
        cancelLocationRequest();
        cancelOperationRequest();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final InboundRepository inboundRepository;
        private final CommonRepository commonRepository;
        private final UhfScanner scanner;
        private final boolean canConfirm;

        public Factory(InboundRepository inboundRepository,
                CommonRepository commonRepository, UhfScanner scanner,
                boolean canConfirm) {
            this.inboundRepository = inboundRepository;
            this.commonRepository = commonRepository;
            this.scanner = scanner;
            this.canConfirm = canConfirm;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(InboundViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new InboundViewModel(inboundRepository, commonRepository,
                    scanner, canConfirm);
        }
    }
}
