package com.onedongua.radiodrive.source.qingting;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.onedongua.radiodrive.R;
import com.onedongua.radiodrive.RadioDroidApp;
import com.onedongua.radiodrive.Utils;
import com.onedongua.radiodrive.interfaces.IFragmentSearchable;
import com.onedongua.radiodrive.source.RadioDataSource;
import com.onedongua.radiodrive.source.RadioDataSourceManager;
import com.onedongua.radiodrive.station.DataRadioStation;
import com.onedongua.radiodrive.station.ItemAdapterStation;
import com.onedongua.radiodrive.station.StationsFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * 蜻蜓FM 电台列表 Fragment。
 * 支持三种模式：分类浏览、推荐、搜索，均支持分页滚动加载。
 */
public class FragmentQingtingStations extends Fragment implements IFragmentSearchable {
    private static final String TAG = "FragmentQingtingStations";

    public static final int MODE_CATEGORY = 0;
    public static final int MODE_FEATURED = 1;
    public static final int MODE_SEARCH = 2;

    public static final String KEY_MODE = "mode";
    public static final String KEY_CATEGORY_ID = "categoryId";
    /**
     * @deprecated 保留供 FragmentQingtingCategories 引用，本 Fragment 不再使用
     */
    public static final String KEY_CATEGORY_TYPE = "categoryType";

    private RecyclerView rvStations;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ItemAdapterStation adapter;
    private LinearLayoutManager layoutManager;

    private int mode = MODE_CATEGORY;
    private String categoryId;
    private int currentPage = 1;
    private int totalCount = 0;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String lastSearchQuery = "";
    private boolean isAttached = false;

    private final List<DataRadioStation> stationList = new ArrayList<>();
    private RadioDataSource source;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 搜索防抖
    private Runnable pendingSearchRunnable = null;
    private static final long SEARCH_DEBOUNCE_MS = 500;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvStations = view.findViewById(R.id.recyclerViewStations);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);

        Bundle args = getArguments();
        if (args != null) {
            mode = args.getInt(KEY_MODE, MODE_CATEGORY);
            categoryId = args.getString(KEY_CATEGORY_ID);
        }

        source = RadioDataSourceManager.getInstance().getCurrentSource();

        adapter = new ItemAdapterStation(getActivity(), R.layout.list_item_station, StationsFilter.FilterType.LOCAL);
        adapter.setStationActionsListener(new ItemAdapterStation.StationActionsListener() {
            @Override
            public void onStationClick(DataRadioStation station, int pos) {
                RadioDroidApp app = (RadioDroidApp) getActivity().getApplication();
                Utils.showPlaySelection(app, station, getActivity().getSupportFragmentManager());
            }

            @Override
            public void onStationSwiped(DataRadioStation station) {
            }

            @Override
            public void onStationMoved(int from, int to) {
            }

            @Override
            public void onStationMoveFinished() {
            }
        });

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvStations.setAdapter(adapter);
        rvStations.setLayoutManager(layoutManager);
        rvStations.addItemDecoration(new DividerItemDecoration(rvStations.getContext(), layoutManager.getOrientation()));

        // 滚动到底部自动加载下一页
        rvStations.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore) return;

                int totalItemCount = layoutManager.getItemCount();
                if (totalItemCount == 0) return;

                int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                // 当滚动到倒数第5项时触发加载
                if (lastVisibleItem >= totalItemCount - 5) {
                    currentPage++;
                    loadData();
                }
            }
        });

        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(() -> {
            resetAndReload();
        });

        loadData();
        isAttached = true;

        return view;
    }

    /**
     * 重置到第一页并重新加载
     */
    private void resetAndReload() {
        currentPage = 1;
        hasMore = true;
        stationList.clear();
        adapter.updateList(null, new ArrayList<>());
        loadData();
    }

    private void loadData() {
        if (isLoading) return;
        isLoading = true;

        if (!(source instanceof QingtingDataSource)) {
            isLoading = false;
            return;
        }
        final QingtingDataSource qds = (QingtingDataSource) source;

        RadioDataSource.StationsCallback callback = new RadioDataSource.StationsCallback() {
            @Override
            public void onResult(List<DataRadioStation> stations, int total) {
                isLoading = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                totalCount = total;
                if (currentPage == 1) {
                    stationList.clear();
                }
                stationList.addAll(stations);

                // 判断是否还有更多数据
                hasMore = stationList.size() < totalCount;

                if (rvStations != null) {
                    rvStations.post(() -> adapter.updateList(null, stationList));
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                // 加载失败时回退页码，下次滚动可重试
                if (currentPage > 1) currentPage--;

                if (getContext() != null) {
                    mainHandler.post(() ->
                            Toast.makeText(getContext(), R.string.error_list_update, Toast.LENGTH_SHORT).show());
                }
                Log.e(TAG, "loadData error", e);
            }
        };

        switch (mode) {
            case MODE_CATEGORY:
                qds.getStationsByCategory(categoryId, currentPage, callback);
                break;
            case MODE_FEATURED:
                qds.getFeaturedStations(currentPage, callback);
                break;
            case MODE_SEARCH:
                if (TextUtils.isEmpty(lastSearchQuery)) {
                    isLoading = false;
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    return;
                }
                qds.search(lastSearchQuery, currentPage, callback);
                break;
            default:
                isLoading = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                break;
        }
    }

    // ===== IFragmentSearchable 实现 =====

    @Override
    public void Search(StationsFilter.SearchStyle searchStyle, String query) {
        if (mode != MODE_SEARCH) return;

        Log.d(TAG, "Search query = " + query);

        // 取消之前的待执行搜索
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
        }

        lastSearchQuery = query;

        // 防抖：延迟执行搜索
        pendingSearchRunnable = () -> {
            pendingSearchRunnable = null;
            resetAndReload();
        };
        mainHandler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isAttached = false;
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
            pendingSearchRunnable = null;
        }
        if (rvStations != null) {
            rvStations.setAdapter(null);
        }
    }
}
