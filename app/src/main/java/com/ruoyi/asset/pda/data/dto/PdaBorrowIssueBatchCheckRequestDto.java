package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 借出预检请求；借用资料与整批资产保持同一业务上下文。 */
public class PdaBorrowIssueBatchCheckRequestDto {
    @SerializedName("borrowerType")
    private final String borrowerType;

    @SerializedName("borrowUserId")
    private final Long borrowUserId;

    @SerializedName("borrowDeptId")
    private final Long borrowDeptId;

    @SerializedName("borrowOrgName")
    private final String borrowOrgName;

    @SerializedName("borrowContactPhone")
    private final String borrowContactPhone;

    @SerializedName("expectedReturnDate")
    private final String expectedReturnDate;

    @SerializedName("identifiers")
    private final List<PdaAssetIdentifyRequest> identifiers;

    public PdaBorrowIssueBatchCheckRequestDto(String borrowerType, Long borrowUserId,
            Long borrowDeptId, String borrowOrgName, String borrowContactPhone,
            String expectedReturnDate, List<PdaAssetIdentifyRequest> identifiers) {
        this.borrowerType = borrowerType;
        this.borrowUserId = borrowUserId;
        this.borrowDeptId = borrowDeptId;
        this.borrowOrgName = borrowOrgName;
        this.borrowContactPhone = borrowContactPhone;
        this.expectedReturnDate = expectedReturnDate;
        this.identifiers = identifiers;
    }
}
