package com.ruoyi.asset.pda.feature.home;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.session.PdaPermissions;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.data.dto.PdaBootstrapDto;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.testing.FakeAuthRepository;
import com.ruoyi.asset.pda.testing.FakeCommonRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeAuthRepository authRepository;
    private FakeCommonRepository commonRepository;
    private HomeViewModel viewModel;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        authRepository.setSessionState(SessionManager.State.VALID);
        commonRepository = new FakeCommonRepository();
        viewModel = new HomeViewModel(authRepository, commonRepository);
    }

    @Test
    public void pendingSessionDoesNotLoadBootstrap() {
        authRepository.setSessionState(SessionManager.State.PENDING_VALIDATION);

        viewModel.initialize();

        assertEquals(0, commonRepository.getBootstrapCount());
    }

    @Test
    public void initializeLoadsBootstrapOnlyOnce() {
        viewModel.initialize();
        viewModel.initialize();

        assertEquals(HomeUiState.Mode.LOADING, state().getMode());
        assertEquals(1, commonRepository.getBootstrapCount());
    }

    @Test
    public void bootstrapMapsExactPermissionsToCards() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put(PdaPermissions.RFID_TAG_ADD, true);
        features.put(PdaPermissions.RFID_BIND, false);
        features.put(PdaPermissions.RFID_UNBIND, true);
        features.put(PdaPermissions.INBOUND_SCAN, true);
        features.put(PdaPermissions.INBOUND_CONFIRM, false);
        features.put(PdaPermissions.INVENTORY_LIST, true);
        features.put(PdaPermissions.INVENTORY_SUBMIT, true);
        viewModel.initialize();

        commonRepository.completeBootstrap(bootstrap(features));

        assertEquals(HomeUiState.Mode.CONTENT, state().getMode());
        assertEquals("ceshi", state().getLoginName());
        assertTrue(state().isShowTagCreate());
        assertFalse(state().isShowRfidBind());
        assertTrue(state().isShowRfidUnbind());
        assertTrue(state().isShowInbound());
        assertFalse(state().isShowInboundConfirm());
        assertTrue(state().isShowInventory());
    }

    @Test
    public void submitPermissionAloneDoesNotExposeInventoryCard() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put(PdaPermissions.INVENTORY_SUBMIT, true);
        viewModel.initialize();

        commonRepository.completeBootstrap(bootstrap(features));

        assertFalse(state().isShowInventory());
        assertFalse(state().isShowTagCreate());
    }

    @Test
    public void inboundConfirmPermissionAloneDoesNotExposeInboundCard() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put(PdaPermissions.INBOUND_CONFIRM, true);
        viewModel.initialize();

        commonRepository.completeBootstrap(bootstrap(features));

        assertFalse(state().isShowInbound());
        assertTrue(state().isShowInboundConfirm());
    }

    @Test
    public void missingCurrentUserIsProtocolError() {
        viewModel.initialize();
        commonRepository.completeBootstrap(new PdaBootstrapDto(
                "2026-07-17 18:30:00", null,
                Collections.<String, List<PdaDictItemDto>>emptyMap(),
                Collections.emptyMap()));

        assertEquals(HomeUiState.Mode.ERROR, state().getMode());
        assertEquals(R.string.api_protocol_error, state().getErrorTextResId());
    }

    @Test
    public void failedBootstrapCanRetry() {
        viewModel.initialize();
        commonRepository.failBootstrap(TestErrors.network());

        assertEquals(HomeUiState.Mode.ERROR, state().getMode());

        viewModel.retry();

        assertEquals(HomeUiState.Mode.LOADING, state().getMode());
        assertEquals(2, commonRepository.getBootstrapCount());
    }

    @Test
    public void logoutIsSingleShotAndShowsProcessing() {
        viewModel.initialize();
        commonRepository.completeBootstrap(bootstrap(Collections.emptyMap()));

        viewModel.logout();
        viewModel.logout();

        assertEquals(HomeUiState.Mode.LOGGING_OUT, state().getMode());
        assertEquals(1, authRepository.getLogoutCount());
    }

    @Test
    public void failedBootstrapAllowsSwitchingAccount() {
        viewModel.initialize();
        commonRepository.failBootstrap(TestErrors.network());

        viewModel.switchAccount();

        assertEquals(1, authRepository.getClearCount());
        assertEquals(SessionManager.State.INVALID, authRepository.getSessionState());
    }

    private HomeUiState state() {
        return viewModel.getUiState().getValue();
    }

    private PdaBootstrapDto bootstrap(Map<String, Boolean> features) {
        PdaUserDto user = new PdaUserDto(102L, "ceshi", "测试用户", 100L,
                "资产管理部", Collections.emptyList());
        return new PdaBootstrapDto("2026-07-17 18:30:00", user,
                Collections.<String, List<PdaDictItemDto>>emptyMap(), features);
    }
}
