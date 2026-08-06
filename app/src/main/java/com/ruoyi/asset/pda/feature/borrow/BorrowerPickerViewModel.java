package com.ruoyi.asset.pda.feature.borrow;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.repository.BorrowRepository;
import com.ruoyi.asset.pda.data.repository.DefaultBorrowRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

/** 借还页面共用的人员搜索；只返回后端确认的启用人员和部门关系。 */
public final class BorrowerPickerViewModel extends ViewModel {
    private final BorrowRepository borrowRepository;
    private final MutableLiveData<BorrowerPickerUiState> uiState =
            new MutableLiveData<>(new BorrowerPickerUiState(
                    BorrowerPickerUiState.Mode.IDLE, null, null));
    private RequestHandle request = RequestHandle.NONE;
    private int requestVersion;

    public BorrowerPickerViewModel(BorrowRepository borrowRepository) {
        if (borrowRepository == null) {
            throw new IllegalArgumentException("借还人员 Repository 不能为空");
        }
        this.borrowRepository = borrowRepository;
    }

    public LiveData<BorrowerPickerUiState> getUiState() {
        return uiState;
    }

    public void search(String keyword) {
        String checkedKeyword = trim(keyword);
        if (checkedKeyword == null) {
            publish(BorrowerPickerUiState.Mode.ERROR, null,
                    "请输入姓名、账号或部门后搜索");
            return;
        }
        if (checkedKeyword.length() > DefaultBorrowRepository.MAX_KEYWORD_LENGTH) {
            publish(BorrowerPickerUiState.Mode.ERROR, null,
                    "搜索关键词长度不能超过 30 个字符");
            return;
        }
        request.cancel();
        int version = ++requestVersion;
        BorrowerPickerUiState previous = uiState.getValue();
        publish(BorrowerPickerUiState.Mode.SEARCHING,
                previous == null ? null : previous.getBorrowers(), null);
        RequestHandle next = borrowRepository.searchBorrowers(checkedKeyword,
                new RepositoryCallback<List<PdaMasterDataDto>>() {
                    @Override
                    public void onSuccess(List<PdaMasterDataDto> data) {
                        if (version != requestVersion) {
                            return;
                        }
                        List<PdaMasterDataDto> valid = new ArrayList<>();
                        if (data != null) {
                            for (PdaMasterDataDto borrower : data) {
                                if (validBorrower(borrower)) {
                                    valid.add(borrower);
                                }
                            }
                        }
                        publish(BorrowerPickerUiState.Mode.CONTENT, valid,
                                valid.isEmpty() ? "未找到可用人员" : null);
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == requestVersion) {
                            publish(BorrowerPickerUiState.Mode.ERROR, null,
                                    error != null && hasText(error.getMessage())
                                            ? error.getMessage() : "人员搜索失败，请重试");
                        }
                    }
                });
        if (version == requestVersion) {
            request = next;
        }
    }

    private void publish(BorrowerPickerUiState.Mode mode,
            List<PdaMasterDataDto> borrowers, String message) {
        uiState.setValue(new BorrowerPickerUiState(mode, borrowers, message));
    }

    private boolean validBorrower(PdaMasterDataDto value) {
        return value != null && value.getId() != null && value.getId() > 0L
                && value.getParentId() != null && value.getParentId() > 0L
                && hasText(value.getCode()) && hasText(value.getName())
                && hasText(value.getParentName());
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String checked = value.trim();
        return checked.isEmpty() ? null : checked;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        request.cancel();
        request = RequestHandle.NONE;
    }

    public static final class Factory implements ViewModelProvider.Factory {
        private final BorrowRepository borrowRepository;

        public Factory(BorrowRepository borrowRepository) {
            this.borrowRepository = borrowRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(BorrowerPickerViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new BorrowerPickerViewModel(borrowRepository);
        }
    }
}
