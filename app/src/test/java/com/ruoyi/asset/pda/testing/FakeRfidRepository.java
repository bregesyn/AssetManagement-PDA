package com.ruoyi.asset.pda.testing;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;
import com.ruoyi.asset.pda.data.repository.RfidRepository;

import java.util.ArrayList;
import java.util.List;

public final class FakeRfidRepository implements RfidRepository {
    private final List<QueryPending> queries = new ArrayList<>();
    private final List<BatchPending> batches = new ArrayList<>();
    private final List<TagPending> binds = new ArrayList<>();
    private final List<TagPending> unbinds = new ArrayList<>();
    private String lastBindAssetCode;
    private String lastBindEpc;
    private Long lastUnbindTagId;

    @Override
    public RequestHandle queryTag(String epcCode,
            RepositoryCallback<PdaRfidTagDto> callback) {
        QueryPending pending = new QueryPending(epcCode, callback);
        queries.add(pending);
        return () -> pending.cancelled = true;
    }

    @Override
    public RequestHandle batchCreate(List<String> epcCodes, String remark,
            RepositoryCallback<RfidTagBatchResultDto> callback) {
        BatchPending pending = new BatchPending(new ArrayList<>(epcCodes), remark, callback);
        batches.add(pending);
        return () -> pending.cancelled = true;
    }

    @Override
    public RequestHandle bind(String assetCode, String epcCode,
            RepositoryCallback<PdaRfidTagDto> callback) {
        lastBindAssetCode = assetCode;
        lastBindEpc = epcCode;
        TagPending pending = new TagPending(callback);
        binds.add(pending);
        return () -> pending.cancelled = true;
    }

    @Override
    public RequestHandle unbind(Long tagId,
            RepositoryCallback<PdaRfidTagDto> callback) {
        lastUnbindTagId = tagId;
        TagPending pending = new TagPending(callback);
        unbinds.add(pending);
        return () -> pending.cancelled = true;
    }

    public int getQueryCount() { return queries.size(); }
    public int getBatchCount() { return batches.size(); }
    public int getBindCount() { return binds.size(); }
    public int getUnbindCount() { return unbinds.size(); }
    public String getLastQueryEpc() { return last(queries).epc; }
    public List<String> getLastBatchEpcs() { return last(batches).epcs; }
    public String getLastBatchRemark() { return last(batches).remark; }
    public String getLastBindAssetCode() { return lastBindAssetCode; }
    public String getLastBindEpc() { return lastBindEpc; }
    public Long getLastUnbindTagId() { return lastUnbindTagId; }

    public void completeLastQuery(PdaRfidTagDto data) { last(queries).callback.onSuccess(data); }
    public void failLastQuery(ApiErrorMapper.ApiError error) { last(queries).callback.onError(error); }
    public void completeLastBatch(RfidTagBatchResultDto data) { last(batches).callback.onSuccess(data); }
    public void failLastBatch(ApiErrorMapper.ApiError error) { last(batches).callback.onError(error); }
    public void completeLastBind(PdaRfidTagDto data) { last(binds).callback.onSuccess(data); }
    public void failLastBind(ApiErrorMapper.ApiError error) { last(binds).callback.onError(error); }
    public void completeLastUnbind(PdaRfidTagDto data) { last(unbinds).callback.onSuccess(data); }
    public void failLastUnbind(ApiErrorMapper.ApiError error) { last(unbinds).callback.onError(error); }
    public void forceCompleteQuery(int index, PdaRfidTagDto data) {
        queries.get(index).callback.onSuccess(data);
    }

    private static <T> T last(List<T> list) { return list.get(list.size() - 1); }

    private static final class QueryPending {
        private final String epc;
        private final RepositoryCallback<PdaRfidTagDto> callback;
        private boolean cancelled;
        private QueryPending(String epc, RepositoryCallback<PdaRfidTagDto> callback) {
            this.epc = epc;
            this.callback = callback;
        }
    }

    private static final class BatchPending {
        private final List<String> epcs;
        private final String remark;
        private final RepositoryCallback<RfidTagBatchResultDto> callback;
        private boolean cancelled;
        private BatchPending(List<String> epcs, String remark,
                RepositoryCallback<RfidTagBatchResultDto> callback) {
            this.epcs = epcs;
            this.remark = remark;
            this.callback = callback;
        }
    }

    private static final class TagPending {
        private final RepositoryCallback<PdaRfidTagDto> callback;
        private boolean cancelled;
        private TagPending(RepositoryCallback<PdaRfidTagDto> callback) {
            this.callback = callback;
        }
    }
}
