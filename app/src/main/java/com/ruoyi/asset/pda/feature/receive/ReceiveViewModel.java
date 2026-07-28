package com.ruoyi.asset.pda.feature.receive;

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
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaReceiveBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultReceiveRepository;
import com.ruoyi.asset.pda.data.repository.ReceiveRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * C6200 现场领用的唯一业务状态持有者：先选人，后采集，停止后预检，再整批确认。
 */
public final class ReceiveViewModel extends BaseUhfViewModel {
    private static final String IDENTIFY_TYPE_EPC = "EPC";
    private static final String IDENTIFY_TYPE_ASSET_CODE = "ASSET_CODE";

    private final ReceiveRepository receiveRepository;
    private final CommonRepository commonRepository;
    private final boolean canConfirm;
    private final MutableLiveData<ReceiveUiState> uiState = new MutableLiveData<>();
    private final Map<String, UhfTagReading> readingsByEpc = new LinkedHashMap<>();
    private final Map<Long, ReceiveAssetItem> assetsById = new LinkedHashMap<>();
    private final Set<String> acceptedIdentifierKeys = new LinkedHashSet<>();
    private final Map<String, ReceiveIssueItem> issuesByKey = new LinkedHashMap<>();

    private RequestHandle bootstrapRequest = RequestHandle.NONE;
    private RequestHandle operationRequest = RequestHandle.NONE;
    private boolean initialized;
    private boolean bootstrapLoaded;
    private boolean initialFailed;
    private int initializationVersion;
    private int operationVersion;
    private ReceiveUiState.Operation operation = ReceiveUiState.Operation.NONE;
    private PdaMasterDataDto selectedRecipient;
    private String operatorName;
    private String serverTime;
    private int duplicateReadCount;
    private String latestEpc;
    private String infoMessage;
    private String errorMessage;
    private PdaReceiveBatchConfirmDto lastConfirmation;
    private int assetCodeClearVersion;
    private int batchResetVersion;
    private long lastTagPublishedAt;

    public ReceiveViewModel(ReceiveRepository receiveRepository,
            CommonRepository commonRepository, UhfScanner scanner,
            boolean canConfirm) {
        super(scanner);
        if (receiveRepository == null || commonRepository == null) {
            throw new IllegalArgumentException("领用 Repository 不能为空");
        }
        this.receiveRepository = receiveRepository;
        this.commonRepository = commonRepository;
        this.canConfirm = canConfirm;
        publish();
    }

    public LiveData<ReceiveUiState> getUiState() {
        return uiState;
    }

    public void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        loadBootstrap();
    }

    public void retryInitialization() {
        if (operation == ReceiveUiState.Operation.NONE && initialFailed) {
            loadBootstrap();
        }
    }

    private void loadBootstrap() {
        bootstrapRequest.cancel();
        int version = ++initializationVersion;
        bootstrapLoaded = false;
        initialFailed = false;
        clearMessages();
        publish();
        RequestHandle request = commonRepository.bootstrap(
                new RepositoryCallback<PdaBootstrapDto>() {
                    @Override
                    public void onSuccess(PdaBootstrapDto data) {
                        if (version != initializationVersion) {
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
            bootstrapRequest = request;
        }
    }

    private void failInitialization(int version, String message) {
        if (version != initializationVersion) {
            return;
        }
        initialFailed = true;
        errorMessage = message;
        bootstrapRequest.cancel();
        bootstrapRequest = RequestHandle.NONE;
        publish();
    }

    /** 调用方在已有待提交内容时先做二次确认，避免人员归属混入同一批次。 */
    public void selectRecipient(PdaMasterDataDto recipient) {
        if (!readyForInput() || operation != ReceiveUiState.Operation.NONE
                || !validRecipient(recipient)
                || sameId(selectedRecipient == null ? null : selectedRecipient.getId(),
                        recipient.getId())) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        selectedRecipient = recipient;
        clearMessages();
        infoMessage = "已选择领用人 " + recipient.getName();
        publish();
    }

    /** F6 和屏幕按钮共用同一批量扫描状态机。 */
    public void toggleScan() {
        if (!readyForInput() || operation != ReceiveUiState.Operation.NONE) {
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
        if (!hasRecipient()) {
            setError("请先选择领用人");
            return;
        }
        if (assetsById.size() >= DefaultReceiveRepository.MAX_BATCH_SIZE) {
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
        List<PdaAssetIdentifyRequest> submitted = new ArrayList<>();
        for (String epc : readingsByEpc.keySet()) {
            submitted.add(new PdaAssetIdentifyRequest(IDENTIFY_TYPE_EPC, epc));
        }
        precheck(submitted, ReceiveUiState.Operation.PRECHECK);
    }

    public void addByAssetCode(String assetCode) {
        if (!readyForInput() || operation != ReceiveUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!hasRecipient()) {
            setError("请先选择领用人");
            return;
        }
        String checkedCode = trim(assetCode);
        if (checkedCode == null) {
            setError("请输入资产编码");
            return;
        }
        if (checkedCode.length() > DefaultReceiveRepository.MAX_ASSET_CODE_LENGTH) {
            setError("资产编码长度不能超过 64 个字符");
            return;
        }
        if (assetsById.size() >= DefaultReceiveRepository.MAX_BATCH_SIZE) {
            setError("当前批次已达到 100 件上限");
            return;
        }
        String key = identifierKey(IDENTIFY_TYPE_ASSET_CODE, checkedCode);
        if (acceptedIdentifierKeys.contains(key)) {
            addLocalDuplicate(IDENTIFY_TYPE_ASSET_CODE, checkedCode,
                    "该资产编码已在当前领用清单中");
            return;
        }
        lastConfirmation = null;
        precheck(singleIdentifier(IDENTIFY_TYPE_ASSET_CODE, checkedCode),
                ReceiveUiState.Operation.ASSET_CODE);
    }

    private List<PdaAssetIdentifyRequest> singleIdentifier(String type, String value) {
        List<PdaAssetIdentifyRequest> identifiers = new ArrayList<>(1);
        identifiers.add(new PdaAssetIdentifyRequest(type, value));
        return identifiers;
    }

    private void precheck(List<PdaAssetIdentifyRequest> submitted,
            ReceiveUiState.Operation nextOperation) {
        cancelOperationRequest();
        int version = ++operationVersion;
        operation = nextOperation;
        clearMessages();
        publish();
        RequestHandle request = receiveRepository.batchCheck(selectedRecipient.getId(),
                selectedRecipient.getParentId(), submitted,
                new RepositoryCallback<PdaReceiveBatchCheckDto>() {
                    @Override
                    public void onSuccess(PdaReceiveBatchCheckDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        if (!validBatchCheck(data, submitted)) {
                            operation = ReceiveUiState.Operation.NONE;
                            errorMessage = "预检响应与本轮标识不一致，请重试";
                            publish();
                            return;
                        }
                        int addedCount = applyBatchCheck(data);
                        if (nextOperation == ReceiveUiState.Operation.PRECHECK) {
                            readingsByEpc.clear();
                            latestEpc = null;
                        } else {
                            assetCodeClearVersion++;
                        }
                        operation = ReceiveUiState.Operation.NONE;
                        infoMessage = "预检完成：加入 " + addedCount
                                + " 件，异常 " + (data.getTotalCount()
                                - data.getEligibleCount()) + " 件";
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = ReceiveUiState.Operation.NONE;
                            errorMessage = messageOf(error,
                                    "批量预检失败，已保留当前标识");
                            publish();
                        }
                    }
                });
        if (version == operationVersion && operation == nextOperation) {
            operationRequest = request;
        }
    }

    public void removeAsset(Long assetId) {
        if (assetId == null || operation != ReceiveUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        ReceiveAssetItem removed = assetsById.remove(assetId);
        if (removed == null) {
            return;
        }
        PdaAssetIdentifyRequest identifier = removed.getIdentifier();
        acceptedIdentifierKeys.remove(identifierKey(identifier.getIdentifyType(),
                identifier.getIdentifyValue()));
        lastConfirmation = null;
        infoMessage = "已从本批次移除 " + removed.getAssetCode();
        errorMessage = null;
        publish();
    }

    public void clearBatch() {
        if (operation != ReceiveUiState.Operation.NONE) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        infoMessage = "当前领用批次已清空";
        errorMessage = null;
        publish();
    }

    public void confirm(String remark) {
        if (!readyForInput() || operation != ReceiveUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!canConfirm) {
            setError("当前账号没有领用确认权限");
            return;
        }
        if (!hasRecipient()) {
            setError("请先选择领用人");
            return;
        }
        if (assetsById.isEmpty()) {
            setError("请至少加入一件可领用资产");
            return;
        }
        String checkedRemark = trim(remark);
        if (checkedRemark != null
                && checkedRemark.length() > DefaultReceiveRepository.MAX_REMARK_LENGTH) {
            setError("交接备注长度不能超过 500 个字符");
            return;
        }
        List<PdaAssetIdentifyRequest> submitted = new ArrayList<>();
        for (ReceiveAssetItem item : assetsById.values()) {
            submitted.add(item.getIdentifier());
        }
        cancelOperationRequest();
        int version = ++operationVersion;
        operation = ReceiveUiState.Operation.CONFIRM;
        clearMessages();
        publish();
        RequestHandle request = receiveRepository.batchConfirm(selectedRecipient.getId(),
                selectedRecipient.getParentId(), submitted, checkedRemark,
                new RepositoryCallback<PdaReceiveBatchConfirmDto>() {
                    @Override
                    public void onSuccess(PdaReceiveBatchConfirmDto data) {
                        if (version != operationVersion) {
                            return;
                        }
                        if (!validConfirmation(data, submitted)) {
                            operation = ReceiveUiState.Operation.NONE;
                            errorMessage = "确认响应与提交资产不一致，请到后台核对领用单";
                            publish();
                            return;
                        }
                        clearWorkingBatch(false);
                        operation = ReceiveUiState.Operation.NONE;
                        lastConfirmation = data;
                        if (hasText(data.getConfirmUserName())) {
                            operatorName = data.getConfirmUserName().trim();
                        }
                        if (hasText(data.getConfirmTime())) {
                            serverTime = data.getConfirmTime().trim();
                        }
                        infoMessage = "领用成功：" + data.getReceiveNo();
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = ReceiveUiState.Operation.NONE;
                            String base = messageOf(error, "领用确认失败");
                            if (error != null && (error.getKind() == ApiErrorMapper.Kind.NETWORK
                                    || error.getKind() == ApiErrorMapper.Kind.TIMEOUT
                                    || error.getKind() == ApiErrorMapper.Kind.PROTOCOL
                                    || error.getKind() == ApiErrorMapper.Kind.SYSTEM)) {
                                base += "；结果可能未知，请先到后台核对领用单，勿直接重复提交";
                            }
                            errorMessage = base;
                            publish();
                        }
                    }
                });
        if (version == operationVersion
                && operation == ReceiveUiState.Operation.CONFIRM) {
            operationRequest = request;
        }
    }

    public boolean hasPendingWork() {
        return !assetsById.isEmpty() || !readingsByEpc.isEmpty()
                || !issuesByKey.isEmpty();
    }

    private int applyBatchCheck(PdaReceiveBatchCheckDto data) {
        int addedCount = 0;
        for (PdaReceiveBatchCheckDto.Row row : data.getRows()) {
            String identifierKey = identifierKey(row.getIdentifyType(),
                    row.getIdentifyValue());
            if ("ELIGIBLE".equals(row.getStatus())) {
                removeIssuesForIdentifier(row.getIdentifyType(), row.getIdentifyValue());
                acceptedIdentifierKeys.add(identifierKey);
                if (assetsById.containsKey(row.getAssetId())) {
                    duplicateReadCount++;
                    issuesByKey.put(issueKey(row), new ReceiveIssueItem(
                            row.getIdentifyType(), row.getIdentifyValue(),
                            row.getAssetCode(), row.getAssetName(), "DUPLICATE",
                            "该资产已在当前领用清单中"));
                } else {
                    assetsById.put(row.getAssetId(), ReceiveAssetItem.fromBatchRow(row));
                    addedCount++;
                }
            } else {
                issuesByKey.put(issueKey(row), new ReceiveIssueItem(row));
            }
        }
        lastConfirmation = null;
        return addedCount;
    }

    private void addLocalDuplicate(String identifyType, String identifyValue,
            String message) {
        duplicateReadCount++;
        issuesByKey.put(identifierKey(identifyType, identifyValue) + "|DUPLICATE",
                new ReceiveIssueItem(identifyType, identifyValue, null, null,
                        "DUPLICATE", message));
        infoMessage = null;
        errorMessage = message;
        publish();
    }

    private boolean validBatchCheck(PdaReceiveBatchCheckDto data,
            List<PdaAssetIdentifyRequest> submitted) {
        if (data == null || data.getRows() == null
                || data.getTotalCount() != submitted.size()
                || data.getRows().size() != submitted.size()
                || data.getEligibleCount() < 0 || data.getIneligibleCount() < 0
                || data.getUnknownCount() < 0 || data.getDuplicateCount() < 0
                || data.getEligibleCount() + data.getIneligibleCount()
                + data.getUnknownCount() + data.getDuplicateCount()
                != data.getTotalCount()) {
            return false;
        }
        Set<Long> eligibleIds = new HashSet<>();
        for (int index = 0; index < submitted.size(); index++) {
            PdaAssetIdentifyRequest identifier = submitted.get(index);
            PdaReceiveBatchCheckDto.Row row = data.getRows().get(index);
            if (row == null || !identifier.getIdentifyType().equals(row.getIdentifyType())
                    || !identifier.getIdentifyValue().equals(row.getIdentifyValue())
                    || !isCheckStatus(row.getStatus())) {
                return false;
            }
            if ("ELIGIBLE".equals(row.getStatus())
                    && (!validBatchAsset(row) || !eligibleIds.add(row.getAssetId()))) {
                return false;
            }
        }
        return true;
    }

    private boolean isCheckStatus(String status) {
        return "ELIGIBLE".equals(status) || "INELIGIBLE".equals(status)
                || "UNKNOWN".equals(status) || "DUPLICATE".equals(status);
    }

    private boolean validBatchAsset(PdaReceiveBatchCheckDto.Row row) {
        return row.getAssetId() != null && row.getAssetId() > 0L
                && hasText(row.getAssetCode()) && hasText(row.getAssetName());
    }

    private boolean validConfirmation(PdaReceiveBatchConfirmDto data,
            List<PdaAssetIdentifyRequest> submitted) {
        if (data == null || data.getOrderId() == null || data.getOrderId() < 1L
                || !hasText(data.getReceiveNo())
                || !hasText(data.getReceiveUserName())
                || !hasText(data.getReceiveDeptName())
                || !hasText(data.getConfirmUserName())
                || !hasText(data.getConfirmTime())
                || data.getReceiveUserId() == null
                || !data.getReceiveUserId().equals(selectedRecipient.getId())
                || data.getReceiveDeptId() == null
                || !data.getReceiveDeptId().equals(selectedRecipient.getParentId())
                || data.getTotalCount() != submitted.size()
                || data.getSuccessCount() != submitted.size()
                || data.getRows() == null
                || data.getRows().size() != submitted.size()) {
            return false;
        }
        Set<Long> expected = new LinkedHashSet<>(assetsById.keySet());
        Set<Long> returned = new HashSet<>();
        for (PdaReceiveBatchConfirmDto.Row row : data.getRows()) {
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
        acceptedIdentifierKeys.clear();
        issuesByKey.clear();
        duplicateReadCount = 0;
        latestEpc = null;
        lastTagPublishedAt = 0L;
        batchResetVersion++;
        if (clearConfirmation) {
            lastConfirmation = null;
        }
    }

    private boolean hasRecipient() {
        return validRecipient(selectedRecipient);
    }

    private boolean validRecipient(PdaMasterDataDto value) {
        return value != null && value.getId() != null && value.getId() > 0L
                && value.getParentId() != null && value.getParentId() > 0L
                && hasText(value.getName()) && hasText(value.getParentName());
    }

    private boolean readyForInput() {
        return bootstrapLoaded && !initialFailed;
    }

    private boolean isScanning() {
        UhfScanState state = getCurrentScanState();
        return state == UhfScanState.SCANNING || state == UhfScanState.PROCESSING;
    }

    private String identifierKey(String type, String value) {
        return String.valueOf(type) + "|" + String.valueOf(value);
    }

    private String issueKey(PdaReceiveBatchCheckDto.Row row) {
        return identifierKey(row.getIdentifyType(), row.getIdentifyValue())
                + "|" + String.valueOf(row.getStatus());
    }

    private void removeIssuesForIdentifier(String type, String value) {
        String prefix = identifierKey(type, value) + "|";
        issuesByKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private boolean sameId(Long first, Long second) {
        return first == null ? second == null : first.equals(second);
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
        boolean initialLoading = !initialFailed && !bootstrapLoaded;
        uiState.setValue(new ReceiveUiState(initialLoading,
                !initialFailed && bootstrapLoaded, initialFailed, canConfirm,
                operation, getCurrentScanState(), operatorName, serverTime,
                selectedRecipient, new ArrayList<>(assetsById.values()),
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
        if (reading == null || operation != ReceiveUiState.Operation.NONE) {
            return;
        }
        String epc = reading.getEpc();
        latestEpc = epc;
        if (acceptedIdentifierKeys.contains(identifierKey(IDENTIFY_TYPE_EPC, epc))) {
            duplicateReadCount++;
        } else if (readingsByEpc.containsKey(epc)) {
            duplicateReadCount++;
            readingsByEpc.put(epc, reading);
        } else {
            readingsByEpc.put(epc, reading);
        }
        int remaining = DefaultReceiveRepository.MAX_BATCH_SIZE - assetsById.size();
        if (readingsByEpc.size() >= remaining) {
            stopScanning();
            publish();
            precheckReadings();
            return;
        }
        // 高频 EPC 读数只合并 UI 刷新，完整集合仍逐条保留给一次性预检。
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

    private void cancelOperationRequest() {
        operationRequest.cancel();
        operationRequest = RequestHandle.NONE;
    }

    @Override
    protected void onViewModelCleared() {
        initializationVersion++;
        operationVersion++;
        bootstrapRequest.cancel();
        bootstrapRequest = RequestHandle.NONE;
        cancelOperationRequest();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final ReceiveRepository receiveRepository;
        private final CommonRepository commonRepository;
        private final UhfScanner scanner;
        private final boolean canConfirm;

        public Factory(ReceiveRepository receiveRepository,
                CommonRepository commonRepository, UhfScanner scanner,
                boolean canConfirm) {
            this.receiveRepository = receiveRepository;
            this.commonRepository = commonRepository;
            this.scanner = scanner;
            this.canConfirm = canConfirm;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ReceiveViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new ReceiveViewModel(receiveRepository, commonRepository,
                    scanner, canConfirm);
        }
    }
}
