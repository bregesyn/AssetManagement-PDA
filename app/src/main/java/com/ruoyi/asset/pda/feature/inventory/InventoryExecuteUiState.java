package com.ruoyi.asset.pda.feature.inventory;

import com.ruoyi.asset.pda.core.uhf.UhfScanState;
import com.ruoyi.asset.pda.core.uhf.UhfTagReading;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行页渲染快照。
 *
 * <p>应盘清单、扫描预判和服务端确认结果刻意分开保存：预判只表达本轮临时事实，
 * 已确认的盘点结果才写入清单状态。</p>
 */
public final class InventoryExecuteUiState {
    public enum Mode { LOADING, READY, PREVIEW, READONLY }

    private final Mode mode;
    private final PdaInventoryTaskDto task;
    private final UhfScanState scanState;
    private final boolean writing;
    private final boolean canSubmit;
    private final boolean readOnly;
    private final List<UhfTagReading> readings;
    private final int duplicateReadCount;
    private final UhfTagReading lastReading;
    private final PdaInventoryBatchScanDto preview;
    private final Set<String> selectedEpcs;
    private final PdaInventoryBatchConfirmDto lastConfirm;
    private final List<PdaMasterDataDto> warehouses;
    private final List<PdaMasterDataDto> locations;
    private final Long selectedWarehouseId;
    private final Long selectedLocationId;
    private final List<PdaInventoryItemDto> expectedItems;
    private final Map<Long, String> itemResultOverrides;
    private final Map<Long, String> previewEpcByItemId;
    private final boolean expectedItemsLoading;
    private final boolean hasMoreExpectedItems;
    private final List<PdaInventorySurplusDto> surpluses;
    private final String infoMessage;
    private final String errorMessage;

    public InventoryExecuteUiState(Mode mode, PdaInventoryTaskDto task,
            UhfScanState scanState, boolean writing, boolean canSubmit, boolean readOnly,
            List<UhfTagReading> readings, int duplicateReadCount, UhfTagReading lastReading,
            PdaInventoryBatchScanDto preview, Set<String> selectedEpcs,
            PdaInventoryBatchConfirmDto lastConfirm, List<PdaMasterDataDto> warehouses,
            List<PdaMasterDataDto> locations, Long selectedWarehouseId,
            Long selectedLocationId, List<PdaInventoryItemDto> expectedItems,
            Map<Long, String> itemResultOverrides, Map<Long, String> previewEpcByItemId,
            boolean expectedItemsLoading, boolean hasMoreExpectedItems,
            List<PdaInventorySurplusDto> surpluses, String infoMessage, String errorMessage) {
        this.mode = mode;
        this.task = task;
        this.scanState = scanState;
        this.writing = writing;
        this.canSubmit = canSubmit;
        this.readOnly = readOnly;
        this.readings = copy(readings);
        this.duplicateReadCount = duplicateReadCount;
        this.lastReading = lastReading;
        this.preview = preview;
        this.selectedEpcs = Collections.unmodifiableSet(new LinkedHashSet<>(selectedEpcs));
        this.lastConfirm = lastConfirm;
        this.warehouses = copy(warehouses);
        this.locations = copy(locations);
        this.selectedWarehouseId = selectedWarehouseId;
        this.selectedLocationId = selectedLocationId;
        this.expectedItems = copy(expectedItems);
        this.itemResultOverrides = copyMap(itemResultOverrides);
        this.previewEpcByItemId = copyMap(previewEpcByItemId);
        this.expectedItemsLoading = expectedItemsLoading;
        this.hasMoreExpectedItems = hasMoreExpectedItems;
        this.surpluses = copy(surpluses);
        this.infoMessage = infoMessage;
        this.errorMessage = errorMessage;
    }

    public static InventoryExecuteUiState loading(boolean canSubmit) {
        return new InventoryExecuteUiState(Mode.LOADING, null, UhfScanState.IDLE,
                false, canSubmit, false, Collections.emptyList(), 0, null, null,
                Collections.emptySet(), null, Collections.emptyList(), Collections.emptyList(),
                null, null, Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap(), false, false, Collections.emptyList(), null, null);
    }

    private static <T> List<T> copy(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values == null
                ? Collections.emptyList() : values));
    }

    private static <K, V> Map<K, V> copyMap(Map<K, V> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values == null
                ? Collections.emptyMap() : values));
    }

    public Mode getMode() { return mode; }
    public PdaInventoryTaskDto getTask() { return task; }
    public UhfScanState getScanState() { return scanState; }
    public boolean isWriting() { return writing; }
    public boolean isCanSubmit() { return canSubmit; }
    public boolean isReadOnly() { return readOnly; }
    public List<UhfTagReading> getReadings() { return readings; }
    public int getDuplicateReadCount() { return duplicateReadCount; }
    public UhfTagReading getLastReading() { return lastReading; }
    public PdaInventoryBatchScanDto getPreview() { return preview; }
    public Set<String> getSelectedEpcs() { return selectedEpcs; }
    public PdaInventoryBatchConfirmDto getLastConfirm() { return lastConfirm; }
    public List<PdaMasterDataDto> getWarehouses() { return warehouses; }
    public List<PdaMasterDataDto> getLocations() { return locations; }
    public Long getSelectedWarehouseId() { return selectedWarehouseId; }
    public Long getSelectedLocationId() { return selectedLocationId; }
    public List<PdaInventoryItemDto> getExpectedItems() { return expectedItems; }
    public Map<Long, String> getItemResultOverrides() { return itemResultOverrides; }
    public Map<Long, String> getPreviewEpcByItemId() { return previewEpcByItemId; }
    public boolean isExpectedItemsLoading() { return expectedItemsLoading; }
    public boolean hasMoreExpectedItems() { return hasMoreExpectedItems; }
    public List<PdaInventorySurplusDto> getSurpluses() { return surpluses; }
    public String getInfoMessage() { return infoMessage; }
    public String getErrorMessage() { return errorMessage; }
}
