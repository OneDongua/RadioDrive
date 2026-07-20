package com.onedongua.radiodrive.source.qingting;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.onedongua.radiodrive.ActivityMain;
import com.onedongua.radiodrive.R;
import com.onedongua.radiodrive.adapters.ItemAdapterCategory;
import com.onedongua.radiodrive.data.DataCategory;
import com.onedongua.radiodrive.source.RadioDataSource;
import com.onedongua.radiodrive.source.RadioDataSourceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 蜻蜓FM 分类/地区列表 Fragment。
 * 点击后跳转到 FragmentQingtingStations 显示对应电台。
 */
public class FragmentQingtingCategories extends Fragment {
    private static final String TAG = "FragmentQingtingCategories";

    public static final String KEY_CATEGORY_TYPE = "categoryType";
    public static final String TYPE_REGIONS = "regions";
    public static final String TYPE_CLASSES = "classes";

    private RecyclerView rvCategories;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ItemAdapterCategory adapter;
    private RadioDataSource.CategoryType categoryType;
    private List<DataCategory> categories = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations_remote, container, false);
        rvCategories = view.findViewById(R.id.recyclerViewStations);
        swipeRefreshLayout = view.findViewById(R.id.swiperefresh);

        Bundle args = getArguments();
        if (args != null) {
            String type = args.getString(KEY_CATEGORY_TYPE, TYPE_REGIONS);
            categoryType = TYPE_CLASSES.equals(type)
                    ? RadioDataSource.CategoryType.CLASSES
                    : RadioDataSource.CategoryType.REGIONS;
        }

        adapter = new ItemAdapterCategory(R.layout.list_item_category);
        adapter.setCategoryClickListener(this::onCategoryClick);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rvCategories.setAdapter(adapter);
        rvCategories.setLayoutManager(llm);
        rvCategories.addItemDecoration(new DividerItemDecoration(rvCategories.getContext(), llm.getOrientation()));

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadCategories);
        }

        loadCategories();

        return view;
    }

    private void loadCategories() {
        RadioDataSourceManager manager = RadioDataSourceManager.getInstance();
        manager.getCurrentSource().getCategories(categoryType, new RadioDataSource.CategoryCallback() {
            @Override
            public void onResult(List<DataCategory> categories) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                FragmentQingtingCategories.this.categories = categories;
                if (rvCategories != null) rvCategories.post(() -> adapter.updateList(categories));
            }

            @Override
            public void onError(Exception e) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.error_list_update, Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "loadCategories error", e);
            }
        });
    }

    private void onCategoryClick(DataCategory category) {
        // category.UsedCount 存储了 regionId 或 classId
        String categoryId = String.valueOf(category.UsedCount);

        FragmentQingtingStations stationsFrag = new FragmentQingtingStations();
        Bundle args = new Bundle();
        args.putString(FragmentQingtingStations.KEY_CATEGORY_ID, categoryId);
        args.putString(FragmentQingtingStations.KEY_CATEGORY_TYPE,
                categoryType == RadioDataSource.CategoryType.CLASSES ? "class" : "region");
        stationsFrag.setArguments(args);

        FragmentTransaction ft = requireActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.containerView, stationsFrag)
                .addToBackStack(String.valueOf(ActivityMain.FRAGMENT_FROM_BACKSTACK))
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvCategories != null) rvCategories.setAdapter(null);
    }
}
