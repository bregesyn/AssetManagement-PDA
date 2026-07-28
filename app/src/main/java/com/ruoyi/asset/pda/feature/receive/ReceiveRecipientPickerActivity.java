package com.ruoyi.asset.pda.feature.receive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaMasterDataDto;
import com.ruoyi.asset.pda.databinding.ActivityReceiveRecipientPickerBinding;

/** 独立人员选择页，服务端只返回启用人员的脱敏身份和所属部门。 */
public final class ReceiveRecipientPickerActivity extends SessionAwareActivity {
    public static final String EXTRA_RECIPIENT_ID = "receive_recipient_id";
    public static final String EXTRA_RECIPIENT_CODE = "receive_recipient_code";
    public static final String EXTRA_RECIPIENT_NAME = "receive_recipient_name";
    public static final String EXTRA_RECIPIENT_DEPT_ID = "receive_recipient_dept_id";
    public static final String EXTRA_RECIPIENT_DEPT_NAME = "receive_recipient_dept_name";

    private ActivityReceiveRecipientPickerBinding binding;
    private ReceiveRecipientPickerViewModel viewModel;
    private ReceiveRecipientAdapter recipientAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiveRecipientPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        viewModel = new ViewModelProvider(this,
                new ReceiveRecipientPickerViewModel.Factory(
                        container.getReceiveRepository()))
                .get(ReceiveRecipientPickerViewModel.class);
        recipientAdapter = new ReceiveRecipientAdapter(this::finishWithRecipient);
        binding.recipientPickerList.setLayoutManager(new LinearLayoutManager(this));
        binding.recipientPickerList.setAdapter(recipientAdapter);
        binding.recipientPickerToolbar.setNavigationOnClickListener(view -> finish());
        binding.recipientPickerSearchButton.setOnClickListener(view -> search());
        binding.recipientPickerKeywordInput.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        search();
                        return true;
                    }
                    return false;
                });
        viewModel.getUiState().observe(this, this::render);
    }

    private void search() {
        CharSequence value = binding.recipientPickerKeywordInput.getText();
        viewModel.search(value == null ? null : value.toString());
    }

    private void render(ReceiveRecipientPickerUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean searching = state.getMode() == ReceiveRecipientPickerUiState.Mode.SEARCHING;
        boolean error = state.getMode() == ReceiveRecipientPickerUiState.Mode.ERROR;
        recipientAdapter.submit(state.getRecipients());
        binding.recipientPickerKeywordInput.setEnabled(!searching);
        binding.recipientPickerSearchButton.setEnabled(!searching);
        binding.recipientPickerSearchButton.setText(searching
                ? R.string.receive_recipient_searching
                : R.string.receive_recipient_search);
        setVisible(binding.recipientPickerProgress, searching);
        setVisible(binding.recipientPickerMessageText, error && hasText(state.getMessage()));
        binding.recipientPickerMessageText.setText(value(state.getMessage()));

        boolean empty = state.getRecipients().isEmpty()
                && state.getMode() != ReceiveRecipientPickerUiState.Mode.SEARCHING;
        setVisible(binding.recipientPickerEmptyText, empty && !error);
        if (empty && !error) {
            binding.recipientPickerEmptyText.setText(
                    state.getMode() == ReceiveRecipientPickerUiState.Mode.CONTENT
                            ? R.string.receive_recipient_no_results
                            : R.string.receive_recipient_picker_empty);
        }
    }

    private void finishWithRecipient(PdaMasterDataDto recipient) {
        if (recipient == null || recipient.getId() == null || recipient.getId() < 1L
                || recipient.getParentId() == null || recipient.getParentId() < 1L) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_RECIPIENT_ID, recipient.getId());
        result.putExtra(EXTRA_RECIPIENT_CODE, recipient.getCode());
        result.putExtra(EXTRA_RECIPIENT_NAME, recipient.getName());
        result.putExtra(EXTRA_RECIPIENT_DEPT_ID, recipient.getParentId());
        result.putExtra(EXTRA_RECIPIENT_DEPT_NAME, recipient.getParentName());
        setResult(RESULT_OK, result);
        finish();
    }

    private String value(String value) {
        return hasText(value) ? value.trim() : getString(R.string.common_unknown);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
