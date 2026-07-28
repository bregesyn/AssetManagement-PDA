package com.ruoyi.asset.pda.feature.receive;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.ruoyi.asset.pda.core.network.ApiErrorMapper;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.data.repository.DefaultReceiveRepository;
import com.ruoyi.asset.pda.data.repository.ReceiveRepository;
import com.ruoyi.asset.pda.data.repository.RepositoryCallback;
import com.ruoyi.asset.pda.data.repository.RequestHandle;

import java.util.ArrayList;
import java.util.List;

/** 搜索全公司启用人员；页面仅持有后端脱敏后的轻量主数据。 */
public final class ReceiveRecipientPickerViewModel extends ViewModel {
    private final ReceiveRepository receiveRepository;
    private final MutableLiveData<ReceiveRecipientPickerUiState> uiState =
            new MutableLiveData<>(new ReceiveRecipientPickerUiState(
                    ReceiveRecipientPickerUiState.Mode.IDLE, null, null));
    private RequestHandle request = RequestHandle.NONE;
    private int requestVersion;

    public ReceiveRecipientPickerViewModel(ReceiveRepository receiveRepository) {
        if (receiveRepository == null) {
            throw new IllegalArgumentException("领用人员 Repository 不能为空");
        }
        this.receiveRepository = receiveRepository;
    }

    public LiveData<ReceiveRecipientPickerUiState> getUiState() {
        return uiState;
    }

    public void search(String keyword) {
        String checkedKeyword = trim(keyword);
        if (checkedKeyword == null) {
            publish(ReceiveRecipientPickerUiState.Mode.ERROR, null,
                    "请输入姓名、登录名或部门后搜索");
            return;
        }
        if (checkedKeyword.length() > DefaultReceiveRepository.MAX_RECIPIENT_KEYWORD_LENGTH) {
            publish(ReceiveRecipientPickerUiState.Mode.ERROR, null,
                    "搜索关键词长度不能超过 30 个字符");
            return;
        }
        request.cancel();
        int version = ++requestVersion;
        ReceiveRecipientPickerUiState previous = uiState.getValue();
        publish(ReceiveRecipientPickerUiState.Mode.SEARCHING,
                previous == null ? null : previous.getRecipients(), null);
        RequestHandle next = receiveRepository.searchRecipients(checkedKeyword,
                new RepositoryCallback<List<PdaMasterDataDto>>() {
                    @Override
                    public void onSuccess(List<PdaMasterDataDto> data) {
                        if (version != requestVersion) {
                            return;
                        }
                        List<PdaMasterDataDto> valid = new ArrayList<>();
                        if (data != null) {
                            for (PdaMasterDataDto value : data) {
                                if (validRecipient(value)) {
                                    valid.add(value);
                                }
                            }
                        }
                        publish(ReceiveRecipientPickerUiState.Mode.CONTENT, valid,
                                valid.isEmpty() ? "未找到可领用人员" : null);
                    }

                    @Override
                    public void onError(ApiErrorMapper.ApiError error) {
                        if (version == requestVersion) {
                            publish(ReceiveRecipientPickerUiState.Mode.ERROR, null,
                                    error != null && hasText(error.getMessage())
                                            ? error.getMessage()
                                            : "人员搜索失败，请重试");
                        }
                    }
                });
        if (version == requestVersion) {
            request = next;
        }
    }

    private void publish(ReceiveRecipientPickerUiState.Mode mode,
            List<PdaMasterDataDto> values, String message) {
        uiState.setValue(new ReceiveRecipientPickerUiState(mode, values, message));
    }

    private boolean validRecipient(PdaMasterDataDto value) {
        return value != null && value.getId() != null && value.getId() > 0L
                && value.getParentId() != null && value.getParentId() > 0L
                && hasText(value.getCode()) && hasText(value.getName())
                && hasText(value.getParentName());
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        private final ReceiveRepository receiveRepository;

        public Factory(ReceiveRepository receiveRepository) {
            this.receiveRepository = receiveRepository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (!modelClass.isAssignableFrom(ReceiveRecipientPickerViewModel.class)) {
                throw new IllegalArgumentException("不支持的 ViewModel 类型");
            }
            return (T) new ReceiveRecipientPickerViewModel(receiveRepository);
        }
    }
}
