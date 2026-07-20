package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.repository.AssetRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

public final class FakeAssetRepository implements AssetRepository {
    private final List<Pending> requests = new ArrayList<>();

    @Override
    public RequestHandle identify(String identifyType, String identifyValue,
            RepositoryCallback<PdaAssetIdentifyDto> callback) {
        Pending pending = new Pending(identifyType, identifyValue, callback);
        requests.add(pending);
        return () -> pending.cancelled = true;
    }

    public int getRequestCount() { return requests.size(); }
    public String getLastType() { return last().type; }
    public String getLastValue() { return last().value; }
    public boolean isCancelled(int index) { return requests.get(index).cancelled; }

    public void completeLast(PdaAssetIdentifyDto data) {
        last().callback.onSuccess(data);
    }

    public void failLast(ApiErrorMapper.ApiError error) {
        last().callback.onError(error);
    }

    public void forceComplete(int index, PdaAssetIdentifyDto data) {
        requests.get(index).callback.onSuccess(data);
    }

    private Pending last() {
        return requests.get(requests.size() - 1);
    }

    private static final class Pending {
        private final String type;
        private final String value;
        private final RepositoryCallback<PdaAssetIdentifyDto> callback;
        private boolean cancelled;

        private Pending(String type, String value,
                RepositoryCallback<PdaAssetIdentifyDto> callback) {
            this.type = type;
            this.value = value;
            this.callback = callback;
        }
    }
}
