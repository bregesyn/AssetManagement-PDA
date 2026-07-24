package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.repository.CommonRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.List;

public final class FakeCommonRepository implements CommonRepository {
    private RepositoryCallback<PdaBootstrapDto> bootstrapCallback;
    private RepositoryCallback<List<PdaMasterDataDto>> warehousesCallback;
    private RepositoryCallback<List<PdaMasterDataDto>> locationsCallback;
    private int bootstrapCount;
    private int warehousesCount;
    private int locationsCount;
    private Long lastWarehouseId;
    private RecordingHandle lastHandle;

    @Override
    public RequestHandle bootstrap(RepositoryCallback<PdaBootstrapDto> callback) {
        bootstrapCount++;
        bootstrapCallback = callback;
        lastHandle = new RecordingHandle();
        return lastHandle;
    }

    @Override
    public RequestHandle dict(String dictType,
            RepositoryCallback<List<PdaDictItemDto>> callback) {
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle warehouses(RepositoryCallback<List<PdaMasterDataDto>> callback) {
        warehousesCount++;
        warehousesCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle locations(Long warehouseId,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
        locationsCount++;
        lastWarehouseId = warehouseId;
        locationsCallback = callback;
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle categories(RepositoryCallback<List<PdaMasterDataDto>> callback) {
        return RequestHandle.NONE;
    }

    public void completeBootstrap(PdaBootstrapDto data) {
        bootstrapCallback.onSuccess(data);
    }

    public void failBootstrap(ApiErrorMapper.ApiError error) {
        bootstrapCallback.onError(error);
    }

    public void completeWarehouses(List<PdaMasterDataDto> data) {
        warehousesCallback.onSuccess(data);
    }

    public void failWarehouses(ApiErrorMapper.ApiError error) {
        warehousesCallback.onError(error);
    }

    public void completeLocations(List<PdaMasterDataDto> data) {
        locationsCallback.onSuccess(data);
    }

    public void failLocations(ApiErrorMapper.ApiError error) {
        locationsCallback.onError(error);
    }

    public int getBootstrapCount() {
        return bootstrapCount;
    }

    public int getWarehousesCount() {
        return warehousesCount;
    }

    public int getLocationsCount() {
        return locationsCount;
    }

    public Long getLastWarehouseId() {
        return lastWarehouseId;
    }

    public boolean isLastRequestCanceled() {
        return lastHandle != null && lastHandle.canceled;
    }

    private static final class RecordingHandle implements RequestHandle {
        private boolean canceled;

        @Override
        public void cancel() {
            canceled = true;
        }
    }
}
