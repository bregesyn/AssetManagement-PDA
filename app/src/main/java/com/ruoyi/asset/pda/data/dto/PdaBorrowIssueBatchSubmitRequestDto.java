package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 借出提交请求；备注只落在借用主单，不扩散到归还明细。 */
public final class PdaBorrowIssueBatchSubmitRequestDto
        extends PdaBorrowIssueBatchCheckRequestDto {
    @SerializedName("remark")
    private final String remark;

    public PdaBorrowIssueBatchSubmitRequestDto(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String borrowExternalContactName, String borrowExternalContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers,
            String remark) {
        super(borrowerType, borrowUserId, borrowDeptId, borrowOrgName,
                borrowContactPhone, borrowExternalContactName, borrowExternalContactPhone,
                expectedReturnDate, identifiers);
        this.remark = remark;
    }
}
