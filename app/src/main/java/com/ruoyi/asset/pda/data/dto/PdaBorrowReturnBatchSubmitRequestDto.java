package com.ruoyi.asset.pda.data.dto;

import java.util.List;

/** 归还提交请求与预检请求保持同一最小字段边界。 */
public final class PdaBorrowReturnBatchSubmitRequestDto
        extends PdaBorrowReturnBatchCheckRequestDto {
    public PdaBorrowReturnBatchSubmitRequestDto(List<PdaAssetIdentifyRequest> identifiers) {
        super(identifiers);
    }
}
