package com.ruoyi.asset.pda.feature.repair;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaDictItemDto;
import com.ruoyi.asset.pda.data.dto.PdaRepairOrderDto;
import com.ruoyi.asset.pda.databinding.ActivityRepairWorkbenchBinding;

import java.util.List;

/** PDA 报修维修的入口：权限决定可见队列，服务端决定每张工单是否能处理。 */
public final class RepairWorkbenchActivity extends SessionAwareActivity {
    public static final String EXTRA_CAN_LIST = "repair_can_list";
    public static final String EXTRA_CAN_SUBMIT = "repair_can_submit";
    public static final String EXTRA_CAN_START = "repair_can_start";
    public static final String EXTRA_CAN_FINISH = "repair_can_finish";
    private static final int REQUEST_SUBMIT = 4101;
    private static final int REQUEST_DETAIL = 4102;

    private ActivityRepairWorkbenchBinding binding;
    private RepairWorkbenchViewModel viewModel;
    private RepairOrderAdapter adapter;
    private boolean canList;
    private boolean canSubmit;
    private boolean canStart;
    private boolean canFinish;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairWorkbenchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        canList = getIntent().getBooleanExtra(EXTRA_CAN_LIST, false);
        canSubmit = getIntent().getBooleanExtra(EXTRA_CAN_SUBMIT, false);
        canStart = getIntent().getBooleanExtra(EXTRA_CAN_START, false);
        canFinish = getIntent().getBooleanExtra(EXTRA_CAN_FINISH, false);
        viewModel = new ViewModelProvider(this, new RepairWorkbenchViewModelFactory(
                container.getRepairRepository(), container.getCommonRepository(), canList,
                canSubmit, canStart, canFinish)).get(RepairWorkbenchViewModel.class);
        adapter = new RepairOrderAdapter(this::openDetail);
        binding.repairOrderRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.repairOrderRecycler.setAdapter(adapter);
        binding.repairWorkbenchToolbar.setNavigationOnClickListener(view -> finish());
        binding.repairSubmitEntry.setOnClickListener(view -> openSubmit());
        binding.repairMineTab.setOnClickListener(view -> viewModel.selectTab(
                RepairWorkbenchUiState.Tab.MINE));
        binding.repairWorkTab.setOnClickListener(view -> viewModel.selectTab(
                RepairWorkbenchUiState.Tab.WORK));
        binding.repairStatusFilter.setOnClickListener(view -> showStatusMenu());
        binding.repairSearchButton.setOnClickListener(view -> search());
        binding.repairKeywordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                search();
                return true;
            }
            return false;
        });
        binding.repairRefresh.setOnClickListener(view -> viewModel.refresh());
        binding.repairLoadMore.setOnClickListener(view -> viewModel.loadMore());
        binding.repairRetry.setOnClickListener(view -> viewModel.retry());
        viewModel.getState().observe(this, this::render);
        viewModel.initialize();
    }

    private void render(RepairWorkbenchUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean initializing = state.getMode() == RepairWorkbenchUiState.Mode.INITIALIZING;
        boolean error = state.getMode() == RepairWorkbenchUiState.Mode.ERROR;
        boolean hasMine = state.isCanList();
        boolean hasWork = state.isCanStart() || state.isCanFinish();
        binding.repairProgress.setVisibility(initializing || state.isLoading() ? View.VISIBLE : View.GONE);
        binding.repairError.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.repairError.setText(error ? RepairUi.displayText(state.getMessage()) : "");
        binding.repairRetry.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.repairContent.setVisibility(error ? View.GONE : View.VISIBLE);
        binding.repairSubmitEntry.setVisibility(state.isCanSubmit() ? View.VISIBLE : View.GONE);
        binding.repairTabs.setVisibility(hasMine && hasWork ? View.VISIBLE : View.GONE);
        binding.repairMineTab.setVisibility(hasMine ? View.VISIBLE : View.GONE);
        binding.repairWorkTab.setVisibility(hasWork ? View.VISIBLE : View.GONE);
        boolean mine = state.getTab() == RepairWorkbenchUiState.Tab.MINE;
        binding.repairMineTab.setSelected(mine);
        binding.repairWorkTab.setSelected(!mine && state.getTab() == RepairWorkbenchUiState.Tab.WORK);
        binding.repairMineTab.setTextColor(getColor(mine ? R.color.pda_primary_dark
                : R.color.pda_text_secondary));
        binding.repairWorkTab.setTextColor(getColor(!mine && state.getTab()
                == RepairWorkbenchUiState.Tab.WORK ? R.color.pda_primary_dark
                : R.color.pda_text_secondary));
        boolean showFilters = state.getTab() != RepairWorkbenchUiState.Tab.NONE;
        binding.repairFilterRow.setVisibility(showFilters ? View.VISIBLE : View.GONE);
        binding.repairStatusFilter.setVisibility(mine ? View.VISIBLE : View.GONE);
        binding.repairStatusFilter.setText(statusFilterText(state.getStatus(), state.getStatuses()));
        if (error || initializing) {
            return;
        }
        adapter.submit(state.getOrders(), state.getStatuses());
        boolean empty = state.getOrders().isEmpty() && !state.isLoading();
        binding.repairEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.repairOrderRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.repairLoadMore.setVisibility(state.isHasMore() ? View.VISIBLE : View.GONE);
        binding.repairLoadMore.setEnabled(!state.isLoadingMore());
        binding.repairLoadMore.setText(state.isLoadingMore()
                ? R.string.repair_loading_more : R.string.repair_load_more);
        binding.repairNoPermissionHint.setVisibility(state.getTab() == RepairWorkbenchUiState.Tab.NONE
                ? View.VISIBLE : View.GONE);
    }

    private void showStatusMenu() {
        RepairWorkbenchUiState state = viewModel.getState().getValue();
        if (state == null || state.getTab() != RepairWorkbenchUiState.Tab.MINE) {
            return;
        }
        PopupMenu menu = new PopupMenu(this, binding.repairStatusFilter);
        menu.getMenu().add(Menu.NONE, 0, 0, R.string.repair_status_all);
        List<PdaDictItemDto> statuses = state.getStatuses();
        for (int index = 0; index < statuses.size(); index++) {
            menu.getMenu().add(Menu.NONE, index + 1, index + 1,
                    statuses.get(index).getLabel());
        }
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            viewModel.setStatus(id == 0 ? null : statuses.get(id - 1).getValue());
            return true;
        });
        menu.show();
    }

    private void search() {
        CharSequence value = binding.repairKeywordInput.getText();
        viewModel.search(value == null ? null : value.toString());
    }

    private String statusFilterText(String status, List<PdaDictItemDto> statuses) {
        String label = RepairUi.hasText(status) ? RepairUi.statusLabel(status, statuses)
                : getString(R.string.repair_status_all);
        return getString(R.string.repair_status_filter, label);
    }

    private void openSubmit() {
        if (!canSubmit) {
            return;
        }
        startActivityForResult(new Intent(this, RepairSubmitActivity.class), REQUEST_SUBMIT);
    }

    private void openDetail(PdaRepairOrderDto order) {
        if (order == null || order.getRepairId() == null || order.getRepairId() < 1L) {
            return;
        }
        Intent intent = new Intent(this, RepairDetailActivity.class);
        intent.putExtra(RepairDetailActivity.EXTRA_REPAIR_ID, order.getRepairId());
        intent.putExtra(RepairDetailActivity.EXTRA_CAN_LIST, canList);
        intent.putExtra(RepairDetailActivity.EXTRA_CAN_START, canStart);
        intent.putExtra(RepairDetailActivity.EXTRA_CAN_FINISH, canFinish);
        startActivityForResult(intent, REQUEST_DETAIL);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_SUBMIT || requestCode == REQUEST_DETAIL) && resultCode == RESULT_OK) {
            viewModel.refresh();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private static final class RepairWorkbenchViewModelFactory
            implements ViewModelProvider.Factory {
        private final com.ruoyi.asset.pda.data.repository.RepairRepository repairRepository;
        private final com.ruoyi.asset.pda.data.repository.CommonRepository commonRepository;
        private final boolean canList;
        private final boolean canSubmit;
        private final boolean canStart;
        private final boolean canFinish;

        private RepairWorkbenchViewModelFactory(
                com.ruoyi.asset.pda.data.repository.RepairRepository repairRepository,
                com.ruoyi.asset.pda.data.repository.CommonRepository commonRepository,
                boolean canList, boolean canSubmit, boolean canStart, boolean canFinish) {
            this.repairRepository = repairRepository;
            this.commonRepository = commonRepository;
            this.canList = canList;
            this.canSubmit = canSubmit;
            this.canStart = canStart;
            this.canFinish = canFinish;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(RepairWorkbenchViewModel.class)) {
                return (T) new RepairWorkbenchViewModel(repairRepository, commonRepository,
                        canList, canSubmit, canStart, canFinish);
            }
            throw new IllegalArgumentException("未知 ViewModel：" + modelClass.getName());
        }
    }
}
