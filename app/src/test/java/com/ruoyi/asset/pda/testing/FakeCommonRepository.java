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
    private int bootstrapCount;
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
        return RequestHandle.NONE;
    }

    @Override
    public RequestHandle locations(Long warehouseId,
            RepositoryCallback<List<PdaMasterDataDto>> callback) {
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

    public int getBootstrapCount() {
        return bootstrapCount;
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
