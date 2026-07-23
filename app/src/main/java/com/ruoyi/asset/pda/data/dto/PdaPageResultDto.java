package com.ruoyi.asset.pda.data.dto;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** 后端统一分页结构，保留服务端顺序供现场任务排序使用。 */
public final class PdaPageResultDto<T> {
    @SerializedName("total")
    private long total;
    @SerializedName("pageNum")
    private int pageNum;
    @SerializedName("pageSize")
    private int pageSize;
    @SerializedName("rows")
    private List<T> rows = new ArrayList<>();

    public PdaPageResultDto() {
    }

    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public List<T> getRows() { return rows; }
}
