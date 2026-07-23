package com.ruoyi.asset.pda.data.api;

import com.ruoyi.asset.pda.core.network.ApiResponse;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyDto;
import com.ruoyi.asset.pda.data.dto.PdaAssetIdentifyRequest;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchConfirmRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchLossDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryBatchScanRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryItemResultRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryScanRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusDto;
import com.ruoyi.asset.pda.data.dto.PdaInventorySurplusRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskActionRequestDto;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.data.dto.PdaPageResultDto;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidBindRequest;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagDto;
import com.ruoyi.asset.pda.data.dto.PdaRfidTagQueryRequest;
import com.ruoyi.asset.pda.data.dto.PdaRfidUnbindRequest;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchCreateRequest;
import com.ruoyi.asset.pda.data.dto.RfidTagBatchResultDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 只声明当前阶段已从真实后端核实并实际使用的 PDA 接口。
 */
public interface PdaApiService {
    @POST("asset/pda/login")
    Call<ApiResponse<PdaUserDto>> login(@Body PdaLoginRequest request);

    @POST("asset/pda/logout")
    Call<ApiResponse<Void>> logout();

    @GET("asset/pda/profile")
    Call<ApiResponse<PdaUserDto>> profile();

    @GET("asset/pda/config/bootstrap")
    Call<ApiResponse<PdaBootstrapDto>> bootstrap();

    @GET("asset/pda/dict/{dictType}")
    Call<ApiResponse<List<PdaDictItemDto>>> dict(@Path("dictType") String dictType);

    @GET("asset/pda/master/warehouses")
    Call<ApiResponse<List<PdaMasterDataDto>>> warehouses();

    @GET("asset/pda/master/locations")
    Call<ApiResponse<List<PdaMasterDataDto>>> locations(
            @Query("warehouseId") Long warehouseId);

    @GET("asset/pda/master/categories")
    Call<ApiResponse<List<PdaMasterDataDto>>> categories();

    @POST("asset/pda/asset/identify")
    Call<ApiResponse<PdaAssetIdentifyDto>> identifyAsset(
            @Body PdaAssetIdentifyRequest request);

    @POST("asset/pda/rfid/tag/query")
    Call<ApiResponse<PdaRfidTagDto>> queryRfidTag(
            @Body PdaRfidTagQueryRequest request);

    @POST("asset/pda/rfid/tags/batch")
    Call<ApiResponse<RfidTagBatchResultDto>> batchCreateRfidTags(
            @Body RfidTagBatchCreateRequest request);

    @POST("asset/pda/rfid/bind")
    Call<ApiResponse<PdaRfidTagDto>> bindRfid(@Body PdaRfidBindRequest request);

    @POST("asset/pda/rfid/unbind")
    Call<ApiResponse<PdaRfidTagDto>> unbindRfid(@Body PdaRfidUnbindRequest request);

    @GET("asset/pda/inventory/tasks")
    Call<ApiResponse<PdaPageResultDto<PdaInventoryTaskDto>>> inventoryTasks(
            @Query("pageNum") int pageNum, @Query("pageSize") int pageSize);

    @GET("asset/pda/inventory/tasks/{taskId}")
    Call<ApiResponse<PdaInventoryTaskDto>> inventoryTask(@Path("taskId") Long taskId);

    @GET("asset/pda/inventory/tasks/{taskId}/items")
    Call<ApiResponse<PdaPageResultDto<PdaInventoryItemDto>>> inventoryItems(
            @Path("taskId") Long taskId, @Query("inventoried") Boolean inventoried,
            @Query("keyword") String keyword, @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize);

    @GET("asset/pda/inventory/tasks/{taskId}/surpluses")
    Call<ApiResponse<PdaPageResultDto<PdaInventorySurplusDto>>> inventorySurpluses(
            @Path("taskId") Long taskId, @Query("pageNum") int pageNum,
            @Query("pageSize") int pageSize);

    @POST("asset/pda/inventory/tasks/{taskId}/scan")
    Call<ApiResponse<PdaInventoryScanDto>> inventoryScan(
            @Path("taskId") Long taskId, @Body PdaInventoryScanRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/batch-scan")
    Call<ApiResponse<PdaInventoryBatchScanDto>> inventoryBatchScan(
            @Path("taskId") Long taskId, @Body PdaInventoryBatchScanRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/batch-confirm")
    Call<ApiResponse<PdaInventoryBatchConfirmDto>> inventoryBatchConfirm(
            @Path("taskId") Long taskId, @Body PdaInventoryBatchConfirmRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/items/{itemId}/result")
    Call<ApiResponse<PdaInventoryItemDto>> inventoryItemResult(
            @Path("taskId") Long taskId, @Path("itemId") Long itemId,
            @Body PdaInventoryItemResultRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/surpluses")
    Call<ApiResponse<PdaInventorySurplusDto>> inventorySaveSurplus(
            @Path("taskId") Long taskId, @Body PdaInventorySurplusRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/surpluses/{surplusId}/remove")
    Call<ApiResponse<Void>> inventoryRemoveSurplus(
            @Path("taskId") Long taskId, @Path("surplusId") Long surplusId);

    @POST("asset/pda/inventory/tasks/{taskId}/items/mark-pending-loss")
    Call<ApiResponse<PdaInventoryBatchLossDto>> inventoryMarkPendingLoss(
            @Path("taskId") Long taskId, @Body PdaInventoryTaskActionRequestDto request);

    @POST("asset/pda/inventory/tasks/{taskId}/submit")
    Call<ApiResponse<PdaInventoryTaskDto>> inventorySubmit(
            @Path("taskId") Long taskId, @Body PdaInventoryTaskActionRequestDto request);
}
