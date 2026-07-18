package com.ruoyi.asset.pda.data.api;

import com.ruoyi.asset.pda.core.network.ApiResponse;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaLoginRequest;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 只声明阶段 2 已核实并实际使用的 PDA 接口。
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
}
