package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** 后端事务完成后的领用单回执，confirmTime 是页面唯一展示的正式领用时间。 */
public final class PdaReceiveBatchConfirmDto {
    @SerializedName("orderId")
    private Long orderId;

    @SerializedName("receiveNo")
    private String receiveNo;

    @SerializedName("receiveUserId")
    private Long receiveUserId;

    @SerializedName("receiveUserName")
    private String receiveUserName;

    @SerializedName("receiveDeptId")
    private Long receiveDeptId;

    @SerializedName("receiveDeptName")
    private String receiveDeptName;

    @SerializedName("confirmUserName")
    private String confirmUserName;

    @SerializedName("confirmTime")
    private String confirmTime;

    @SerializedName("orderStatus")
    private String orderStatus;

    @SerializedName("totalCount")
    private int totalCount;

    @SerializedName("successCount")
    private int successCount;

    @SerializedName("rows")
    private List<Row> rows;

    public PdaReceiveBatchConfirmDto() {
    }

    public PdaReceiveBatchConfirmDto(Long orderId, String receiveNo,
            Long receiveUserId, String receiveUserName, Long receiveDeptId,
            String receiveDeptName, String confirmUserName, String confirmTime,
            String orderStatus, int totalCount, int successCount, List<Row> rows) {
        this.orderId = orderId;
        this.receiveNo = receiveNo;
        this.receiveUserId = receiveUserId;
        this.receiveUserName = receiveUserName;
        this.receiveDeptId = receiveDeptId;
        this.receiveDeptName = receiveDeptName;
        this.confirmUserName = confirmUserName;
        this.confirmTime = confirmTime;
        this.orderStatus = orderStatus;
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.rows = rows;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReceiveNo() {
        return receiveNo;
    }

    public Long getReceiveUserId() {
        return receiveUserId;
    }

    public String getReceiveUserName() {
        return receiveUserName;
    }

    public Long getReceiveDeptId() {
        return receiveDeptId;
    }

    public String getReceiveDeptName() {
        return receiveDeptName;
    }

    public String getConfirmUserName() {
        return confirmUserName;
    }

    public String getConfirmTime() {
        return confirmTime;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public List<Row> getRows() {
        return rows;
    }

    public static final class Row {
        @SerializedName("assetId")
        private Long assetId;

        @SerializedName("assetCode")
        private String assetCode;

        @SerializedName("assetName")
        private String assetName;

        @SerializedName("status")
        private String status;

        public Row() {
        }

        public Row(Long assetId, String assetCode, String assetName,
                String status) {
            this.assetId = assetId;
            this.assetCode = assetCode;
            this.assetName = assetName;
            this.status = status;
        }

        public Long getAssetId() {
            return assetId;
        }

        public String getAssetCode() {
            return assetCode;
        }

        public String getAssetName() {
            return assetName;
        }

        public String getStatus() {
            return status;
        }
    }
}
