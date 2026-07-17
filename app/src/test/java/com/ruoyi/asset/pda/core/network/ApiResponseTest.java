package com.ruoyi.asset.pda.core.network;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ApiResponseTest {
    @Test
    public void parsesRealAjaxResultFieldNames() {
        ApiResponse<?> response = new Gson().fromJson(
                "{\"code\":0,\"msg\":\"操作成功\",\"data\":{}}", ApiResponse.class);

        assertEquals(Integer.valueOf(0), response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertTrue(response.isSuccess());
        assertFalse(response.isSessionExpired());
    }

    @Test
    public void acceptsAjaxResultWithoutDataField() {
        ApiResponse<?> response = new Gson().fromJson(
                "{\"code\":401,\"msg\":\"登录状态已失效，请重新登录\"}", ApiResponse.class);

        assertTrue(response.isSessionExpired());
        assertNull(response.getData());
    }
}
