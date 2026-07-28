package com.ruoyi.asset.pda.feature.receive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.testing.FakeReceiveRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;

public class ReceiveRecipientPickerViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private FakeReceiveRepository repository;
    private ReceiveRecipientPickerViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeReceiveRepository();
        viewModel = new ReceiveRecipientPickerViewModel(repository);
    }

    @Test
    public void blankKeywordDoesNotCallServer() {
        viewModel.search("  ");

        assertEquals(0, repository.getRecipientsCount());
        assertEquals(ReceiveRecipientPickerUiState.Mode.ERROR, state().getMode());
    }

    @Test
    public void resultKeepsOnlyUsableRecipientFields() {
        viewModel.search(" 张 ");
        assertEquals("张", repository.getLastKeyword());
        repository.completeRecipients(Arrays.asList(
                new PdaMasterDataDto(7L, "zhangsan", "张三", 9L, "资产部"),
                new PdaMasterDataDto(8L, "bad", "无部门", null, null)));

        assertEquals(ReceiveRecipientPickerUiState.Mode.CONTENT, state().getMode());
        assertEquals(1, state().getRecipients().size());
        assertEquals("张三", state().getRecipients().get(0).getName());
        assertTrue(state().getMessage() == null);
    }

    private ReceiveRecipientPickerUiState state() {
        return viewModel.getUiState().getValue();
    }
}
