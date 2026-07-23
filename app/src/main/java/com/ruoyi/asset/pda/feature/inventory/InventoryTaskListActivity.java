package com.ruoyi.asset.pda.feature.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ruoyi.asset.pda.AssetPdaApplication;
import com.ruoyi.asset.pda.BuildConfig;
import com.ruoyi.asset.pda.R;
import com.ruoyi.asset.pda.app.AppContainer;
import com.ruoyi.asset.pda.core.ui.SessionAwareActivity;
import com.ruoyi.asset.pda.data.dto.PdaInventoryTaskDto;
import com.ruoyi.asset.pda.databinding.ActivityInventoryTaskListBinding;

public final class InventoryTaskListActivity extends SessionAwareActivity {
    public static final String EXTRA_CAN_SUBMIT = "inventory_can_submit";
    static final String EXTRA_SKIP_INITIAL_LOAD = "inventory_skip_initial_load";

    private ActivityInventoryTaskListBinding binding;
    private InventoryTaskListViewModel viewModel;
    private InventoryTaskAdapter adapter;
    private boolean readonlyTab;
    private boolean canSubmit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInventoryTaskListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppContainer container = ((AssetPdaApplication) getApplication()).getAppContainer();
        if (!initializeSessionGuard(container.getSessionManager())) {
            return;
        }
        canSubmit = getIntent().getBooleanExtra(EXTRA_CAN_SUBMIT, false);
        viewModel = new ViewModelProvider(this,
                new InventoryTaskListViewModel.Factory(container.getInventoryRepository()))
                .get(InventoryTaskListViewModel.class);
        adapter = new InventoryTaskAdapter(this::openTask);
        binding.inventoryTaskRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.inventoryTaskRecycler.setAdapter(adapter);
        binding.inventoryTaskRefresh.setOnClickListener(view -> viewModel.refresh());
        binding.inventoryTaskRetry.setOnClickListener(view -> viewModel.refresh());
        binding.inventoryTaskLoadMore.setOnClickListener(view -> viewModel.loadMore());
        binding.inventoryTaskActionTab.setOnClickListener(view -> selectTab(false));
        binding.inventoryTaskReadonlyTab.setOnClickListener(view -> selectTab(true));
        binding.inventoryTaskToolbar.setNavigationOnClickListener(view -> finish());
        viewModel.getUiState().observe(this, this::render);
        // 仪器测试只验证首屏控件可触达，Debug 下允许隔离真实后端；正式包始终自动加载任务列表。
        if (!BuildConfig.DEBUG || !getIntent().getBooleanExtra(EXTRA_SKIP_INITIAL_LOAD, false)) {
            viewModel.initialize();
        }
    }

    private void selectTab(boolean readonly) {
        if (readonlyTab == readonly) {
            return;
        }
        readonlyTab = readonly;
        InventoryTaskListUiState state = viewModel.getUiState().getValue();
        if (state != null) {
            render(state);
        }
    }

    private void render(InventoryTaskListUiState state) {
        if (state == null || binding == null) {
            return;
        }
        boolean loading = state.getMode() == InventoryTaskListUiState.Mode.LOADING;
        boolean error = state.getMode() == InventoryTaskListUiState.Mode.ERROR;
        binding.inventoryTaskProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.inventoryTaskError.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.inventoryTaskError.setText(error ? valueOrDash(state.getErrorMessage()) : "");
        binding.inventoryTaskRetry.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.inventoryTaskContent.setVisibility(error ? View.GONE : View.VISIBLE);
        if (error) {
            return;
        }
        selectTabAppearance();
        if (loading) {
            return;
        }
        if (readonlyTab) {
            adapter.submit(state.getReadonlyTasks(), true);
            binding.inventoryTaskCount.setText(getString(R.string.inventory_task_readonly_count,
                    state.getReadonlyTasks().size()));
        } else {
            adapter.submit(state.getActionableTasks(), false);
            binding.inventoryTaskCount.setText(getString(R.string.inventory_task_actionable_count,
                    state.getActionableTasks().size()));
        }
        boolean empty = adapter.getItemCount() == 0;
        binding.inventoryTaskEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.inventoryTaskRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.inventoryTaskLoadMore.setVisibility(state.isHasMore() ? View.VISIBLE : View.GONE);
        binding.inventoryTaskLoadMore.setEnabled(!state.isLoadingMore());
        binding.inventoryTaskLoadMore.setText(state.isLoadingMore()
                ? R.string.inventory_task_loading_more : R.string.inventory_task_load_more);
    }

    private void selectTabAppearance() {
        binding.inventoryTaskActionTab.setSelected(!readonlyTab);
        binding.inventoryTaskReadonlyTab.setSelected(readonlyTab);
        binding.inventoryTaskActionTab.setTextColor(getColor(readonlyTab
                ? R.color.pda_text_secondary : R.color.pda_primary_dark));
        binding.inventoryTaskReadonlyTab.setTextColor(getColor(readonlyTab
                ? R.color.pda_primary_dark : R.color.pda_text_secondary));
    }

    private void openTask(PdaInventoryTaskDto task) {
        if (task == null || task.getTaskId() == null) {
            Toast.makeText(this, R.string.inventory_task_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, InventoryExecuteActivity.class);
        intent.putExtra(InventoryExecuteActivity.EXTRA_TASK_ID, task.getTaskId());
        intent.putExtra(InventoryExecuteActivity.EXTRA_TASK_NO, task.getTaskNo());
        intent.putExtra(InventoryExecuteActivity.EXTRA_CAN_SUBMIT, canSubmit && !readonlyTab);
        startActivity(intent);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? getString(R.string.home_unknown_value) : value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
