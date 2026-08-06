package com.ruoyi.asset.pda.feature.borrow;

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
import com.ruoyi.asset.pda.data.dto.PdaBorrowBatchCheckDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowIssueBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaBorrowReturnBatchSubmitDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.repository.BorrowRepository;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.DefaultBorrowRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PDA 借出/归还唯一业务状态持有者：先准备业务资料，再连续采集，停止后批量预检。
 */
public final class BorrowReturnViewModel extends BaseUhfViewModel {
    public static final String DICT_BORROWER_TYPE = "ams_borrower_type";
    public static final String STATUS_ELIGIBLE = "ELIGIBLE";
    public static final String STATUS_INELIGIBLE = "INELIGIBLE";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_PENDING_RETURN_CONFIRM = "PENDING_RETURN_CONFIRM";

    private final BorrowRepository borrowRepository;
    private final CommonRepository commonRepository;
    private final boolean canIssueScan;
    private final boolean canIssueSubmit;
    private final boolean canReturnScan;
    private final boolean canReturnSubmit;
    private final MutableLiveData<BorrowReturnUiState> uiState = new MutableLiveData<>();
    private final Map<String, UhfTagReading> readingsByEpc = new LinkedHashMap<>();
    private final Map<Long, BorrowAssetItem> assetsById = new LinkedHashMap<>();
    private final Set<String> acceptedIdentifierKeys = new LinkedHashSet<>();
    private final Map<String, BorrowIssueItem> issuesByKey = new LinkedHashMap<>();

    private RequestHandle bootstrapRequest = RequestHandle.NONE;
    private RequestHandle operationRequest = RequestHandle.NONE;
    private int initializationVersion;
    private int operationVersion;
    private boolean initialized;
    private boolean bootstrapLoaded;
    private boolean initialFailed;
    private BorrowReturnUiState.Mode mode;
    private BorrowReturnUiState.Operation operation = BorrowReturnUiState.Operation.NONE;
    private String operatorName;
    private String serverTime;
    private List<PdaDictItemDto> borrowerTypes = new ArrayList<>();
    private String borrowerType = DefaultBorrowRepository.BORROWER_TYPE_INTERNAL;
    private PdaMasterDataDto selectedBorrower;
    private String externalOrgName;
    private String externalContactPhone;
    private String expectedReturnDate;
    private int duplicateReadCount;
    private String latestEpc;
    private String infoMessage;
    private String errorMessage;
    private PdaBorrowIssueBatchSubmitDto lastIssueSubmission;
    private PdaBorrowReturnBatchSubmitDto lastReturnSubmission;
    private int assetCodeClearVersion;
    private int batchResetVersion;
    private long lastTagPublishedAt;

    public BorrowReturnViewModel(BorrowRepository borrowRepository,
            CommonRepository commonRepository, UhfScanner scanner, boolean canIssueScan,
            boolean canIssueSubmit, boolean canReturnScan, boolean canReturnSubmit) {
        super(scanner);
        if (borrowRepository == null || commonRepository == null) {
            throw new IllegalArgumentException("借还 Repository 不能为空");
        }
        this.borrowRepository = borrowRepository;
        this.commonRepository = commonRepository;
        this.canIssueScan = canIssueScan;
        this.canIssueSubmit = canIssueSubmit;
        this.canReturnScan = canReturnScan;
        this.canReturnSubmit = canReturnSubmit;
        mode = canIssueScan || !canReturnScan
                ? BorrowReturnUiState.Mode.ISSUE : BorrowReturnUiState.Mode.RETURN;
        publish();
    }

    public LiveData<BorrowReturnUiState> getUiState() {
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
        if (initialFailed && operation == BorrowReturnUiState.Operation.NONE) {
            loadBootstrap();
        }
    }

    public void setMode(BorrowReturnUiState.Mode nextMode) {
        if (nextMode == null || nextMode == mode || operation != BorrowReturnUiState.Operation.NONE
                || (nextMode == BorrowReturnUiState.Mode.ISSUE && !canIssueScan)
                || (nextMode == BorrowReturnUiState.Mode.RETURN && !canReturnScan)) {
            return;
        }
        cancelScanning();
        operationVersion++;
        clearWorkingBatch(true);
        clearIssueForm();
        mode = nextMode;
        clearMessages();
        publish();
    }

    public void setBorrowerType(String value) {
        String checked = trim(value);
        if (!isBorrowerType(checked) || operation != BorrowReturnUiState.Operation.NONE) {
            return;
        }
        borrowerType = checked.toUpperCase(Locale.ROOT);
        if (DefaultBorrowRepository.BORROWER_TYPE_INTERNAL.equals(borrowerType)) {
            externalOrgName = null;
            externalContactPhone = null;
        } else if (selectedBorrower != null) {
            // 外部借用仍以选中的启用人员作为内部联系人，手机号从服务端人员快照带出，避免现场重复录入或错填。
            externalContactPhone = trim(selectedBorrower.getPhoneNumber());
        }
        clearMessages();
        publish();
    }

    public void selectBorrower(PdaMasterDataDto value) {
        if (!validBorrower(value) || operation != BorrowReturnUiState.Operation.NONE) {
            return;
        }
        if (sameBorrower(selectedBorrower, value)) {
            if (DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(borrowerType)) {
                externalContactPhone = trim(value.getPhoneNumber());
                publish();
            }
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        selectedBorrower = value;
        if (DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(borrowerType)) {
            externalContactPhone = trim(value.getPhoneNumber());
        }
        clearMessages();
        infoMessage = "已选择" + borrowerLabel() + " " + value.getName();
        publish();
    }

    public void setExternalOrgName(String value) {
        externalOrgName = trim(value);
    }

    public void setExternalContactPhone(String value) {
        externalContactPhone = trim(value);
    }

    public void setExpectedReturnDate(String value) {
        expectedReturnDate = trim(value);
        clearMessages();
        publish();
    }

    /** 屏幕按钮与 C6200 F6 共用此入口，停止后才发起一次批量预检。 */
    public void toggleScan() {
        if (!readyForInput() || operation != BorrowReturnUiState.Operation.NONE) {
            return;
        }
        if (!canScanCurrentMode()) {
            setError("当前账号没有本模式的扫描权限");
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
        if (assetsById.size() >= DefaultBorrowRepository.MAX_BATCH_SIZE) {
            setError("当前批次已达到 100 件上限");
            return;
        }
        if (mode == BorrowReturnUiState.Mode.ISSUE && !validIssueForm()) {
            return;
        }
        lastIssueSubmission = null;
        lastReturnSubmission = null;
        clearMessages();
        startScanning(UhfScanMode.BATCH);
    }

    public void onScanKeyPressed() {
        toggleScan();
    }

    public void addByAssetCode(String assetCode) {
        if (!readyForInput() || operation != BorrowReturnUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!canScanCurrentMode()) {
            setError("当前账号没有本模式的扫描权限");
            return;
        }
        if (mode == BorrowReturnUiState.Mode.ISSUE && !validIssueForm()) {
            return;
        }
        String checkedCode = trim(assetCode);
        if (checkedCode == null) {
            setError("请输入资产编码");
            return;
        }
        if (checkedCode.length() > DefaultBorrowRepository.MAX_ASSET_CODE_LENGTH) {
            setError("资产编码长度不能超过 64 个字符");
            return;
        }
        if (assetsById.size() + readingsByEpc.size()
                >= DefaultBorrowRepository.MAX_BATCH_SIZE) {
            setError("当前批次已达到 100 件上限");
            return;
        }
        String key = identifierKey(DefaultBorrowRepository.IDENTIFY_TYPE_ASSET_CODE,
                checkedCode);
        if (acceptedIdentifierKeys.contains(key)) {
            addLocalDuplicate(DefaultBorrowRepository.IDENTIFY_TYPE_ASSET_CODE,
                    checkedCode, "该资产编码已在当前批次中");
            return;
        }
        lastIssueSubmission = null;
        lastReturnSubmission = null;
        precheck(singleIdentifier(DefaultBorrowRepository.IDENTIFY_TYPE_ASSET_CODE,
                checkedCode), BorrowReturnUiState.Operation.ASSET_CODE);
    }

    private void loadBootstrap() {
        bootstrapRequest.cancel();
        int version = ++initializationVersion;
        bootstrapLoaded = false;
        initialFailed = false;
        clearMessages();
        publish();
        RequestHandle request = commonRepository.bootstrap(new RepositoryCallback<PdaBootstrapDto>() {
            @Override
            public void onSuccess(PdaBootstrapDto data) {
                if (version != initializationVersion) {
                    return;
                }
                PdaUserDto user = data == null ? null : data.getCurrentUser();
                if (user == null || user.getUserId() == null || !hasText(user.getLoginName())) {
                    failInitialization(version, "当前用户信息不完整，请重新登录");
                    return;
                }
                List<PdaDictItemDto> options = borrowerOptions(data);
                if (options.isEmpty()) {
                    failInitialization(version, "借用类型配置缺失，请联系管理员");
                    return;
                }
                borrowerTypes = options;
                borrowerType = defaultBorrowerType(options);
                operatorName = hasText(user.getUserName())
                        ? user.getUserName().trim() : user.getLoginName().trim();
                serverTime = data.getServerTime();
                bootstrapLoaded = true;
                publish();
            }

            @Override
            public void onError(ApiErrorMapper.ApiError error) {
                failInitialization(version, messageOf(error, "当前用户信息加载失败"));
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

    private void precheckReadings() {
        if (readingsByEpc.isEmpty()) {
            infoMessage = "本轮未读取到 RFID 标签";
            publish();
            return;
        }
        List<PdaAssetIdentifyRequest> submitted = new ArrayList<>();
        for (String epc : readingsByEpc.keySet()) {
            submitted.add(new PdaAssetIdentifyRequest(
                    DefaultBorrowRepository.IDENTIFY_TYPE_EPC, epc));
        }
        precheck(submitted, BorrowReturnUiState.Operation.PRECHECK);
    }

    private void precheck(List<PdaAssetIdentifyRequest> submitted,
            BorrowReturnUiState.Operation nextOperation) {
        cancelOperationRequest();
        final BorrowReturnUiState.Mode requestMode = mode;
        int version = ++operationVersion;
        operation = nextOperation;
        clearMessages();
        publish();
        RepositoryCallback<PdaBorrowBatchCheckDto> callback =
                new RepositoryCallback<PdaBorrowBatchCheckDto>() {
                    @Override
                    public void onSuccess(PdaBorrowBatchCheckDto data) {
                        if (version != operationVersion || requestMode != mode) {
                            return;
                        }
                        if (!validBatchCheck(data, submitted, requestMode)) {
                            operation = BorrowReturnUiState.Operation.NONE;
                            errorMessage = "预检响应与本轮标识不一致，请重试";
                            publish();
                            return;
                        }
                        int addedCount = applyBatchCheck(data);
                        if (nextOperation == BorrowReturnUiState.Operation.PRECHECK) {
                            readingsByEpc.clear();
                            latestEpc = null;
                        } else {
                            assetCodeClearVersion++;
                        }
                        operation = BorrowReturnUiState.Operation.NONE;
                        infoMessage = "预检完成：加入 " + addedCount + " 件，异常 "
                                + (data.getTotalCount() - data.getEligibleCount()) + " 件";
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == operationVersion) {
                            operation = BorrowReturnUiState.Operation.NONE;
                            errorMessage = messageOf(error, "批量预检失败，已保留当前标识");
                            publish();
                        }
                    }
                };
        RequestHandle request;
        if (requestMode == BorrowReturnUiState.Mode.ISSUE) {
            request = borrowRepository.batchCheckIssue(borrowerType,
                    selectedBorrower.getId(), selectedBorrower.getParentId(),
                    externalOrgName, externalContactPhone, expectedReturnDate,
                    submitted, callback);
        } else {
            request = borrowRepository.batchCheckReturn(submitted, callback);
        }
        if (version == operationVersion && operation == nextOperation) {
            operationRequest = request;
        }
    }

    private int applyBatchCheck(PdaBorrowBatchCheckDto data) {
        int addedCount = 0;
        for (PdaBorrowBatchCheckDto.Row row : data.getRows()) {
            String key = identifierKey(row.getIdentifyType(), row.getIdentifyValue());
            if (STATUS_ELIGIBLE.equals(row.getStatus())) {
                removeIssuesForIdentifier(row.getIdentifyType(), row.getIdentifyValue());
                if (assetsById.containsKey(row.getAssetId())) {
                    duplicateReadCount++;
                    issuesByKey.put(issueKey(row), new BorrowIssueItem(
                            row.getIdentifyType(), row.getIdentifyValue(), row.getAssetCode(),
                            row.getAssetName(), STATUS_DUPLICATE,
                            "该资产已在当前批次中"));
                } else {
                    acceptedIdentifierKeys.add(key);
                    assetsById.put(row.getAssetId(), BorrowAssetItem.fromRow(row));
                    addedCount++;
                }
            } else {
                issuesByKey.put(issueKey(row), new BorrowIssueItem(row));
            }
        }
        lastIssueSubmission = null;
        lastReturnSubmission = null;
        return addedCount;
    }

    public void removeAsset(Long assetId) {
        if (assetId == null || operation != BorrowReturnUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        BorrowAssetItem removed = assetsById.remove(assetId);
        if (removed == null) {
            return;
        }
        PdaAssetIdentifyRequest identifier = removed.getIdentifier();
        acceptedIdentifierKeys.remove(identifierKey(identifier.getIdentifyType(),
                identifier.getIdentifyValue()));
        lastIssueSubmission = null;
        lastReturnSubmission = null;
        infoMessage = "已从本批次移除 " + value(removed.getAssetCode());
        errorMessage = null;
        publish();
    }

    public void clearBatch() {
        if (operation != BorrowReturnUiState.Operation.NONE) {
            return;
        }
        cancelScanning();
        clearWorkingBatch(true);
        clearIssueForm();
        infoMessage = "当前批次已清空";
        errorMessage = null;
        publish();
    }

    public void submit(String remark) {
        if (!readyForInput() || operation != BorrowReturnUiState.Operation.NONE
                || isScanning()) {
            return;
        }
        if (!canSubmitCurrentMode()) {
            setError("当前账号没有提交本模式申请的权限");
            return;
        }
        if (assetsById.isEmpty()) {
            setError("请至少加入一件可处理资产");
            return;
        }
        String checkedRemark = trim(remark);
        if (mode == BorrowReturnUiState.Mode.ISSUE && checkedRemark != null
                && checkedRemark.length() > DefaultBorrowRepository.MAX_REMARK_LENGTH) {
            setError("备注长度不能超过 500 个字符");
            return;
        }
        if (mode == BorrowReturnUiState.Mode.ISSUE && !validIssueForm()) {
            return;
        }
        List<PdaAssetIdentifyRequest> submitted = new ArrayList<>();
        for (BorrowAssetItem item : assetsById.values()) {
            submitted.add(item.getIdentifier());
        }
        cancelOperationRequest();
        final BorrowReturnUiState.Mode requestMode = mode;
        int version = ++operationVersion;
        operation = BorrowReturnUiState.Operation.SUBMIT;
        clearMessages();
        publish();
        RepositoryCallback<PdaBorrowIssueBatchSubmitDto> issueCallback =
                new RepositoryCallback<PdaBorrowIssueBatchSubmitDto>() {
                    @Override
                    public void onSuccess(PdaBorrowIssueBatchSubmitDto data) {
                        if (version != operationVersion || requestMode != mode) {
                            return;
                        }
                        if (!validIssueSubmission(data, submitted)) {
                            showUnknownSubmitResult("借出");
                            return;
                        }
                        clearWorkingBatch(false);
                        operation = BorrowReturnUiState.Operation.NONE;
                        lastIssueSubmission = data;
                        infoMessage = "借出申请已提交，等待审批：" + value(data.getBorrowNo());
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        handleSubmitError(version, error, "借出");
                    }
                };
        RepositoryCallback<PdaBorrowReturnBatchSubmitDto> returnCallback =
                new RepositoryCallback<PdaBorrowReturnBatchSubmitDto>() {
                    @Override
                    public void onSuccess(PdaBorrowReturnBatchSubmitDto data) {
                        if (version != operationVersion || requestMode != mode) {
                            return;
                        }
                        if (!validReturnSubmission(data, submitted)) {
                            showUnknownSubmitResult("归还");
                            return;
                        }
                        clearWorkingBatch(false);
                        operation = BorrowReturnUiState.Operation.NONE;
                        lastReturnSubmission = data;
                        infoMessage = "归还申请已提交，等待审批";
                        errorMessage = null;
                        publish();
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        handleSubmitError(version, error, "归还");
                    }
                };
        RequestHandle request;
        if (requestMode == BorrowReturnUiState.Mode.ISSUE) {
            request = borrowRepository.batchSubmitIssue(borrowerType,
                    selectedBorrower.getId(), selectedBorrower.getParentId(),
                    externalOrgName, externalContactPhone, expectedReturnDate,
                    submitted, checkedRemark, issueCallback);
        } else {
            request = borrowRepository.batchSubmitReturn(submitted, returnCallback);
        }
        if (version == operationVersion && operation == BorrowReturnUiState.Operation.SUBMIT) {
            operationRequest = request;
        }
    }

    private void handleSubmitError(int version, ApiErrorMapper.ApiError error,
            String operationName) {
        if (version != operationVersion) {
            return;
        }
        operation = BorrowReturnUiState.Operation.NONE;
        String message = messageOf(error, operationName + "申请提交失败");
        if (error != null && (error.getKind() == ApiErrorMapper.Kind.NETWORK
                || error.getKind() == ApiErrorMapper.Kind.TIMEOUT
                || error.getKind() == ApiErrorMapper.Kind.PROTOCOL
                || error.getKind() == ApiErrorMapper.Kind.SYSTEM)) {
            message += "；结果可能未知，请先到后台核对，勿直接重复提交";
        }
        errorMessage = message;
        publish();
    }

    private void showUnknownSubmitResult(String operationName) {
        operation = BorrowReturnUiState.Operation.NONE;
        errorMessage = operationName + "提交回执与当前清单不一致，请到后台核对，勿重复提交";
        publish();
    }

    private boolean validBatchCheck(PdaBorrowBatchCheckDto data,
            List<PdaAssetIdentifyRequest> submitted, BorrowReturnUiState.Mode requestMode) {
        if (data == null || data.getRows() == null
                || data.getTotalCount() != submitted.size()
                || data.getRows().size() != submitted.size()
                || data.getEligibleCount() < 0 || data.getIneligibleCount() < 0
                || data.getUnknownCount() < 0 || data.getDuplicateCount() < 0
                || data.getEligibleCount() + data.getIneligibleCount()
                + data.getUnknownCount() + data.getDuplicateCount() != data.getTotalCount()) {
            return false;
        }
        for (int index = 0; index < submitted.size(); index++) {
            PdaAssetIdentifyRequest identifier = submitted.get(index);
            PdaBorrowBatchCheckDto.Row row = data.getRows().get(index);
            if (row == null || !identifier.getIdentifyType().equals(row.getIdentifyType())
                    || !identifier.getIdentifyValue().equals(row.getIdentifyValue())
                    || !isCheckStatus(row.getStatus())) {
                return false;
            }
            if (STATUS_ELIGIBLE.equals(row.getStatus())
                    && (!validBatchAsset(row, requestMode))) {
                return false;
            }
        }
        return true;
    }

    private boolean validIssueSubmission(PdaBorrowIssueBatchSubmitDto data,
            List<PdaAssetIdentifyRequest> submitted) {
        if (data == null || data.getOrderId() == null || data.getOrderId() < 1L
                || !hasText(data.getBorrowNo())
                || !DefaultBorrowRepository.BORROWER_TYPE_INTERNAL.equals(data.getBorrowerType())
                && !DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(data.getBorrowerType())
                || !"PENDING_CONFIRM".equals(data.getOrderStatus())
                || data.getBorrowUserId() == null || selectedBorrower == null
                || !data.getBorrowUserId().equals(selectedBorrower.getId())
                || data.getBorrowDeptId() == null
                || !data.getBorrowDeptId().equals(selectedBorrower.getParentId())
                || data.getApprovalTask() == null
                || data.getApprovalTask().getTaskId() == null
                || data.getApprovalTask().getTaskId() < 1L
                || !"PENDING".equals(data.getApprovalTask().getTaskStatus())
                || data.getTotalCount() != submitted.size()
                || data.getSuccessCount() != submitted.size()
                || data.getRows() == null || data.getRows().size() != submitted.size()) {
            return false;
        }
        Set<Long> expected = new LinkedHashSet<>(assetsById.keySet());
        Set<Long> returned = new HashSet<>();
        for (PdaBorrowIssueBatchSubmitDto.Row row : data.getRows()) {
            if (row == null || row.getAssetId() == null
                    || !"SUCCESS".equals(row.getStatus())
                    || !expected.contains(row.getAssetId()) || !returned.add(row.getAssetId())) {
                return false;
            }
        }
        return returned.equals(expected);
    }

    private boolean validReturnSubmission(PdaBorrowReturnBatchSubmitDto data,
            List<PdaAssetIdentifyRequest> submitted) {
        if (data == null || data.getTotalCount() != submitted.size()
                || data.getSuccessCount() != submitted.size()
                || data.getRows() == null || data.getRows().size() != submitted.size()) {
            return false;
        }
        Set<Long> expected = new LinkedHashSet<>(assetsById.keySet());
        Set<Long> returned = new HashSet<>();
        for (PdaBorrowReturnBatchSubmitDto.Row row : data.getRows()) {
            BorrowAssetItem expectedItem = assetsById.get(row == null ? null : row.getAssetId());
            if (row == null || row.getOrderId() == null || row.getAssetId() == null
                || row.getItemId() == null
                || !expected.contains(row.getAssetId())
                || !returned.add(row.getAssetId())
                || !STATUS_PENDING_RETURN_CONFIRM.equals(row.getReturnStatus())
                || expectedItem == null || !row.getOrderId().equals(expectedItem.getOrderId())
                || !row.getItemId().equals(expectedItem.getItemId())
                || row.getApprovalTask() == null
                    || row.getApprovalTask().getTaskId() == null
                    || row.getApprovalTask().getTaskId() < 1L) {
                return false;
            }
        }
        return returned.equals(expected);
    }

    private boolean validBatchAsset(PdaBorrowBatchCheckDto.Row row,
            BorrowReturnUiState.Mode requestMode) {
        if (row.getAssetId() == null || row.getAssetId() < 1L
                || !hasText(row.getAssetCode()) || !hasText(row.getAssetName())) {
            return false;
        }
        return requestMode != BorrowReturnUiState.Mode.RETURN
                || (row.getOrderId() != null && row.getOrderId() > 0L
                && row.getItemId() != null && row.getItemId() > 0L
                && row.getBeforeWarehouseId() != null && row.getBeforeWarehouseId() > 0L
                && row.getBeforeLocationId() != null && row.getBeforeLocationId() > 0L);
    }

    private boolean isCheckStatus(String status) {
        return STATUS_ELIGIBLE.equals(status) || STATUS_INELIGIBLE.equals(status)
                || STATUS_UNKNOWN.equals(status) || STATUS_DUPLICATE.equals(status);
    }

    private void addLocalDuplicate(String identifyType, String identifyValue,
            String message) {
        duplicateReadCount++;
        issuesByKey.put(identifierKey(identifyType, identifyValue) + "|DUPLICATE",
                new BorrowIssueItem(identifyType, identifyValue, null, null,
                        STATUS_DUPLICATE, message));
        infoMessage = null;
        errorMessage = message;
        publish();
    }

    private void clearWorkingBatch(boolean clearSubmission) {
        readingsByEpc.clear();
        assetsById.clear();
        acceptedIdentifierKeys.clear();
        issuesByKey.clear();
        duplicateReadCount = 0;
        latestEpc = null;
        lastTagPublishedAt = 0L;
        assetCodeClearVersion++;
        batchResetVersion++;
        if (clearSubmission) {
            lastIssueSubmission = null;
            lastReturnSubmission = null;
        }
    }

    /**
     * 模式切换或整批清空后不保留借用上下文，避免上一笔借出资料误用于下一笔现场作业。
     */
    private void clearIssueForm() {
        selectedBorrower = null;
        externalOrgName = null;
        externalContactPhone = null;
        expectedReturnDate = null;
        borrowerType = defaultBorrowerType(borrowerTypes);
    }

    private boolean validIssueForm() {
        if (!validBorrower(selectedBorrower)) {
            setError("请先选择借用人或内部联系人");
            return false;
        }
        if (!hasText(expectedReturnDate)) {
            setError("请选择预计归还日期");
            return false;
        }
        if (!dateOnOrAfterServerDate(expectedReturnDate)) {
            setError("预计归还日期不能早于服务器日期");
            return false;
        }
        if (DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(borrowerType)
                && !hasText(externalOrgName)) {
            setError("请填写外部公司名称");
            return false;
        }
        if (DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(borrowerType)
                && !hasText(externalContactPhone)) {
            setError("请填写联系电话");
            return false;
        }
        return true;
    }

    /**
     * 日期选择器只负责交互约束，提交前仍以服务端日期为准，避免设备时间或恢复数据绕过门槛。
     */
    private boolean dateOnOrAfterServerDate(String value) {
        if (!hasText(value) || !hasText(serverTime)) {
            return false;
        }
        String expected = value.trim();
        String server = serverTime.trim();
        if (expected.length() != 10 || server.length() < 10
                || expected.charAt(4) != '-' || expected.charAt(7) != '-'
                || server.charAt(4) != '-' || server.charAt(7) != '-') {
            return false;
        }
        return expected.compareTo(server.substring(0, 10)) >= 0;
    }

    private boolean canScanCurrentMode() {
        return mode == BorrowReturnUiState.Mode.ISSUE ? canIssueScan : canReturnScan;
    }

    private boolean canSubmitCurrentMode() {
        return mode == BorrowReturnUiState.Mode.ISSUE ? canIssueSubmit : canReturnSubmit;
    }

    private boolean readyForInput() {
        return bootstrapLoaded && !initialFailed;
    }

    private boolean isScanning() {
        UhfScanState state = getCurrentScanState();
        return state == UhfScanState.SCANNING || state == UhfScanState.PROCESSING;
    }

    private boolean validBorrower(PdaMasterDataDto value) {
        return value != null && value.getId() != null && value.getId() > 0L
                && value.getParentId() != null && value.getParentId() > 0L
                && hasText(value.getCode()) && hasText(value.getName())
                && hasText(value.getParentName());
    }

    private boolean sameBorrower(PdaMasterDataDto first, PdaMasterDataDto second) {
        return first != null && second != null && first.getId() != null
                && first.getId().equals(second.getId())
                && first.getParentId() != null && first.getParentId().equals(second.getParentId());
    }

    private String borrowerLabel() {
        return DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(borrowerType)
                ? "内部联系人" : "借用人";
    }

    private List<PdaDictItemDto> borrowerOptions(PdaBootstrapDto data) {
        List<PdaDictItemDto> values = data == null || data.getDicts() == null
                ? null : data.getDicts().get(DICT_BORROWER_TYPE);
        List<PdaDictItemDto> result = new ArrayList<>();
        if (values != null) {
            for (PdaDictItemDto value : values) {
                if (value != null && isBorrowerType(value.getValue()) && hasText(value.getLabel())) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private String defaultBorrowerType(List<PdaDictItemDto> values) {
        for (PdaDictItemDto value : values) {
            if ("Y".equalsIgnoreCase(value.getIsDefault())) {
                return value.getValue().trim().toUpperCase(Locale.ROOT);
            }
        }
        return DefaultBorrowRepository.BORROWER_TYPE_INTERNAL;
    }

    private boolean isBorrowerType(String value) {
        if (value == null) {
            return false;
        }
        String checked = value.trim().toUpperCase(Locale.ROOT);
        return DefaultBorrowRepository.BORROWER_TYPE_INTERNAL.equals(checked)
                || DefaultBorrowRepository.BORROWER_TYPE_EXTERNAL.equals(checked);
    }

    private List<PdaAssetIdentifyRequest> singleIdentifier(String type, String value) {
        List<PdaAssetIdentifyRequest> result = new ArrayList<>(1);
        result.add(new PdaAssetIdentifyRequest(type, value));
        return result;
    }

    private String identifierKey(String type, String value) {
        return String.valueOf(type) + "|" + String.valueOf(value);
    }

    private String issueKey(PdaBorrowBatchCheckDto.Row row) {
        return identifierKey(row.getIdentifyType(), row.getIdentifyValue())
                + "|" + String.valueOf(row.getStatus());
    }

    private void removeIssuesForIdentifier(String type, String value) {
        String prefix = identifierKey(type, value) + "|";
        issuesByKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private void cancelOperationRequest() {
        operationRequest.cancel();
        operationRequest = RequestHandle.NONE;
    }

    private void clearMessages() {
        infoMessage = null;
        errorMessage = null;
    }

    private void setError(String message) {
        errorMessage = message;
        infoMessage = null;
        publish();
    }

    private String messageOf(ApiErrorMapper.ApiError error, String fallback) {
        return error != null && hasText(error.getMessage()) ? error.getMessage() : fallback;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String checked = value.trim();
        return checked.isEmpty() ? null : checked;
    }

    private String value(String value) {
        return hasText(value) ? value.trim() : "未知";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void publish() {
        uiState.setValue(new BorrowReturnUiState(!initialFailed && !bootstrapLoaded,
                !initialFailed && bootstrapLoaded, initialFailed, canIssueScan,
                canIssueSubmit, canReturnScan, canReturnSubmit, mode, operation,
                getCurrentScanState(), operatorName, serverTime, borrowerTypes,
                borrowerType, selectedBorrower, externalOrgName, externalContactPhone,
                expectedReturnDate, new ArrayList<>(assetsById.values()),
                new ArrayList<>(issuesByKey.values()), readingsByEpc.size(),
                duplicateReadCount, latestEpc, infoMessage, errorMessage,
                lastIssueSubmission, lastReturnSubmission, assetCodeClearVersion,
                batchResetVersion));
    }

    @Override
    protected void onScannerStateChanged(UhfScanState scanState) {
        publish();
    }

    @Override
    protected void onScannerTagRead(UhfTagReading reading) {
        if (reading == null || operation != BorrowReturnUiState.Operation.NONE
                || !canScanCurrentMode()) {
            return;
        }
        String epc = reading.getEpc();
        latestEpc = epc;
        if (acceptedIdentifierKeys.contains(identifierKey(
                DefaultBorrowRepository.IDENTIFY_TYPE_EPC, epc))) {
            duplicateReadCount++;
        } else if (readingsByEpc.containsKey(epc)) {
            duplicateReadCount++;
            readingsByEpc.put(epc, reading);
        } else {
            readingsByEpc.put(epc, reading);
        }
        int remaining = DefaultBorrowRepository.MAX_BATCH_SIZE - assetsById.size();
        if (readingsByEpc.size() >= remaining) {
            stopScanning();
            publish();
            precheckReadings();
            return;
        }
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

    @Override
    protected void onViewModelCleared() {
        initializationVersion++;
        operationVersion++;
        bootstrapRequest.cancel();
        bootstrapRequest = RequestHandle.NONE;
        cancelOperationRequest();
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final BorrowRepository borrowRepository;
        private final CommonRepository commonRepository;
        private final UhfScanner scanner;
        private final boolean canIssueScan;
        private final boolean canIssueSubmit;
        private final boolean canReturnScan;
        private final boolean canReturnSubmit;

        public Factory(BorrowRepository borrowRepository, CommonRepository commonRepository,
                UhfScanner scanner, boolean canIssueScan, boolean canIssueSubmit,
                boolean canReturnScan, boolean canReturnSubmit) {
            this.borrowRepository = borrowRepository;
            this.commonRepository = commonRepository;
            this.scanner = scanner;
            this.canIssueScan = canIssueScan;
            this.canIssueSubmit = canIssueSubmit;
            this.canReturnScan = canReturnScan;
            this.canReturnSubmit = canReturnSubmit;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(BorrowReturnViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new BorrowReturnViewModel(borrowRepository, commonRepository, scanner,
                    canIssueScan, canIssueSubmit, canReturnScan, canReturnSubmit);
        }
    }
}
