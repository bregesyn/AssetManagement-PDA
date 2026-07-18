package com.ruoyi.asset.pda.feature.login;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.core.session.SessionManager;
import com.ruoyi.asset.pda.core.ui.Event;
import com.ruoyi.asset.pda.data.dto.PdaUserDto;
import com.ruoyi.asset.pda.testing.FakeAuthRepository;
import com.ruoyi.asset.pda.testing.TestErrors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LoginViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeAuthRepository repository;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeAuthRepository();
        viewModel = new LoginViewModel(repository);
    }

    @Test
    public void invalidSessionShowsLoginFormWithoutRequest() {
        repository.setSessionState(SessionManager.State.INVALID);

        viewModel.initialize();

        assertEquals(LoginUiState.Mode.FORM, state().getMode());
        assertEquals(0, repository.getProfileCount());
    }

    @Test
    public void validInProcessSessionNavigatesWithoutProfileRequest() {
        repository.setSessionState(SessionManager.State.VALID);

        viewModel.initialize();

        assertEquals(LoginViewModel.Destination.HOME, nextDestination());
        assertEquals(0, repository.getProfileCount());
    }

    @Test
    public void pendingSessionProfilesThenNavigates() {
        repository.setSessionState(SessionManager.State.PENDING_VALIDATION);

        viewModel.initialize();
        assertEquals(LoginUiState.Mode.RECOVERING, state().getMode());
        assertEquals(1, repository.getProfileCount());

        repository.completeProfile(user());

        assertEquals(LoginViewModel.Destination.HOME, nextDestination());
    }

    @Test
    public void recoveryNetworkFailureKeepsRetryFlow() {
        repository.setSessionState(SessionManager.State.PENDING_VALIDATION);
        viewModel.initialize();

        repository.failProfile(TestErrors.network());

        assertEquals(LoginUiState.Mode.RECOVERY_ERROR, state().getMode());
        assertEquals(0, repository.getClearCount());

        viewModel.retryRecovery();

        assertEquals(LoginUiState.Mode.RECOVERING, state().getMode());
        assertEquals(2, repository.getProfileCount());
    }

    @Test
    public void expiredRecoveryReturnsToForm() {
        repository.setSessionState(SessionManager.State.PENDING_VALIDATION);
        viewModel.initialize();

        repository.failProfile(TestErrors.sessionExpired());

        assertEquals(LoginUiState.Mode.FORM, state().getMode());
        assertEquals("登录状态已失效，请重新登录", state().getErrorMessage());
    }

    @Test
    public void recoveryProtocolFailureAfterSessionClearedReturnsToForm() {
        repository.setSessionState(SessionManager.State.PENDING_VALIDATION);
        viewModel.initialize();
        repository.setSessionState(SessionManager.State.INVALID);

        repository.failProfile(TestErrors.protocol());

        assertEquals(LoginUiState.Mode.FORM, state().getMode());
    }

    @Test
    public void localValidationRejectsMissingCredentials() {
        viewModel.initialize();

        viewModel.login("   ", "password");
        assertEquals(R.string.login_username_required, state().getErrorTextResId());

        viewModel.login("ceshi", "");
        assertEquals(R.string.login_password_required, state().getErrorTextResId());
        assertEquals(0, repository.getLoginCount());
    }

    @Test
    public void loginTrimsUsernamePreservesPasswordAndOmitsDeviceNumber() {
        viewModel.initialize();

        viewModel.login("  ceshi  ", " password ");

        assertEquals(LoginUiState.Mode.SUBMITTING, state().getMode());
        assertEquals("ceshi", repository.getLoginRequest().getUsername());
        assertEquals(" password ", repository.getLoginRequest().getPassword());
        assertNull(repository.getLoginRequest().getDeviceNo());

        repository.completeLogin(user());
        assertEquals(LoginViewModel.Destination.HOME, nextDestination());
    }

    @Test
    public void duplicateLoginIsIgnoredWhileSubmitting() {
        viewModel.initialize();

        viewModel.login("ceshi", "password");
        viewModel.login("ceshi", "password");

        assertEquals(1, repository.getLoginCount());
    }

    @Test
    public void switchAccountCancelsRecoveryAndIgnoresLateSuccess() {
        repository.setSessionState(SessionManager.State.PENDING_VALIDATION);
        viewModel.initialize();

        viewModel.switchAccount();

        assertTrue(repository.isLastRequestCanceled());
        assertEquals(1, repository.getClearCount());
        assertEquals(LoginUiState.Mode.FORM, state().getMode());

        repository.completeProfile(user());

        assertNull(viewModel.getNavigation().getValue());
    }

    @Test
    public void loginFailureReturnsBackendMessageAndAllowsRetry() {
        viewModel.initialize();
        viewModel.login("ceshi", "wrong");

        repository.failLogin(TestErrors.business("用户或密码错误"));

        assertEquals(LoginUiState.Mode.FORM, state().getMode());
        assertEquals("用户或密码错误", state().getErrorMessage());
        assertFalse(repository.isLastRequestCanceled());
    }

    private LoginUiState state() {
        return viewModel.getUiState().getValue();
    }

    private LoginViewModel.Destination nextDestination() {
        Event<LoginViewModel.Destination> event = viewModel.getNavigation().getValue();
        return event == null ? null : event.getContentIfNotHandled();
    }

    private PdaUserDto user() {
        return new PdaUserDto(102L, "ceshi", "测试用户", 100L,
                "资产管理部", Collections.emptyList());
    }
}
