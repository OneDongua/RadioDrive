package com.onedongua.radiodrive;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.onedongua.radiodrive.interfaces.IFragmentRefreshable;
import com.onedongua.radiodrive.interfaces.IFragmentSearchable;
import com.onedongua.radiodrive.source.RadioDataSourceManager;
import com.onedongua.radiodrive.source.qingting.FragmentQingtingCategories;
import com.onedongua.radiodrive.source.qingting.FragmentQingtingStations;
import com.onedongua.radiodrive.station.FragmentStations;
import com.onedongua.radiodrive.station.StationsFilter;

import java.util.ArrayList;
import java.util.List;

public class FragmentTabs extends Fragment implements IFragmentRefreshable, IFragmentSearchable {
    private String itsAdressWWWLocal = "json/stations/bycountrycodeexact/internet?order=clickcount&reverse=true";
    private String itsAdressWWWTopClick = "json/stations/topclick/100";
    private String itsAdressWWWTopVote = "json/stations/topvote/100";
    private String itsAdressWWWChangedLately = "json/stations/lastchange/100";
    private String itsAdressWWWCurrentlyHeard = "json/stations/lastclick/100";
    private String itsAdressWWWTags = "json/tags";
    private String itsAdressWWWCountries = "json/countrycodes";
    private String itsAdressWWWLanguages = "json/languages";

    // RadioBrowser tab indices
    private static final int IDX_LOCAL = 0;
    private static final int IDX_TOP_CLICK = 1;
    private static final int IDX_TOP_VOTE = 2;
    private static final int IDX_CHANGED_LATELY = 3;
    private static final int IDX_CURRENTLY_HEARD = 4;
    private static final int IDX_TAGS = 5;
    private static final int IDX_COUNTRIES = 6;
    private static final int IDX_LANGUAGES = 7;
    private static final int IDX_SEARCH = 8;

    // Qingting tab indices
    private static final int QT_IDX_FEATURED = 0;
    private static final int QT_IDX_REGIONS = 1;
    private static final int QT_IDX_CLASSES = 2;
    private static final int QT_IDX_SEARCH = 3;

    public static ViewPager viewPager;

    private String queuedSearchQuery;
    private StationsFilter.SearchStyle queuedSearchStyle;

    // Dynamic arrays — size depends on data source
    private Fragment[] fragments;
    private String[] addresses;

    // RadioBrowser-specific
    private String[] rbAddresses = new String[]{
            itsAdressWWWLocal,
            itsAdressWWWTopClick,
            itsAdressWWWTopVote,
            itsAdressWWWChangedLately,
            itsAdressWWWCurrentlyHeard,
            itsAdressWWWTags,
            itsAdressWWWCountries,
            itsAdressWWWLanguages,
            ""
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View x = inflater.inflate(R.layout.layout_tabs, null);
        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        viewPager = (ViewPager) x.findViewById(R.id.viewpager);

        setupViewPager(viewPager);

        if (queuedSearchQuery != null) {
            Log.d("TABS", "do queued search by name:" + queuedSearchQuery);
            Search(queuedSearchStyle, queuedSearchQuery);
            queuedSearchQuery = null;
            queuedSearchStyle = StationsFilter.SearchStyle.ByName;
        }

        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                if (getContext() != null)
                    tabLayout.setupWithViewPager(viewPager);
            }
        });

        return x;
    }

    @Override
    public void onResume() {
        super.onResume();

        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        tabLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        final TabLayout tabLayout = getActivity().findViewById(R.id.tabs);
        tabLayout.setVisibility(View.GONE);
    }

    private String getCountryCode() {
        Context ctx = getContext();
        String countryCode = null;
        if (ctx != null) {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            countryCode = tm.getNetworkCountryIso();
            Log.d("MAIN", "Network country code: '" + countryCode + "'");
            if (countryCode != null && countryCode.length() == 2) {
                return countryCode;
            }
            countryCode = tm.getSimCountryIso();
            Log.d("MAIN", "Sim country code: '" + countryCode + "'");
            if (countryCode != null && countryCode.length() == 2) {
                return countryCode;
            }
            countryCode = ctx.getResources().getConfiguration().locale.getCountry();
            rbAddresses[IDX_LOCAL] = "json/stations/bycountrycodeexact/?order=clickcount&reverse=true";
            Log.d("MAIN", "Locale: '" + countryCode + "'");
            if (countryCode != null && countryCode.length() == 2) {
                return countryCode;
            }
        }
        return null;
    }

    private boolean isQingtingSource() {
        try {
            RadioDataSourceManager manager = RadioDataSourceManager.getInstance();
            return manager.getCurrentSource().getSourceType().equals("qingting");
        } catch (Exception e) {
            return false;
        }
    }

    private void setupRadioBrowserTabs(ViewPager viewPager) {
        String countryCode = getCountryCode();
        if (countryCode != null) {
            rbAddresses[IDX_LOCAL] = "json/stations/bycountrycodeexact/" + countryCode + "?order=clickcount&reverse=true";
        }
        addresses = rbAddresses;
        fragments = new Fragment[9];

        fragments[IDX_LOCAL] = new FragmentStations();
        fragments[IDX_TOP_CLICK] = new FragmentStations();
        fragments[IDX_TOP_VOTE] = new FragmentStations();
        fragments[IDX_CHANGED_LATELY] = new FragmentStations();
        fragments[IDX_CURRENTLY_HEARD] = new FragmentStations();
        fragments[IDX_TAGS] = new FragmentCategories();
        fragments[IDX_COUNTRIES] = new FragmentCategories();
        fragments[IDX_LANGUAGES] = new FragmentCategories();
        fragments[IDX_SEARCH] = new FragmentStations();

        for (int i = 0; i < fragments.length; i++) {
            Bundle bundle = new Bundle();
            bundle.putString("url", addresses[i]);
            if (i == IDX_SEARCH) {
                bundle.putBoolean(FragmentStations.KEY_SEARCH_ENABLED, true);
            }
            fragments[i].setArguments(bundle);
        }

        ((FragmentCategories) fragments[IDX_TAGS]).EnableSingleUseFilter(true);
        ((FragmentCategories) fragments[IDX_TAGS]).SetBaseSearchLink(StationsFilter.SearchStyle.ByTagExact);
        ((FragmentCategories) fragments[IDX_COUNTRIES]).SetBaseSearchLink(StationsFilter.SearchStyle.ByCountryCodeExact);
        ((FragmentCategories) fragments[IDX_LANGUAGES]).SetBaseSearchLink(StationsFilter.SearchStyle.ByLanguageExact);

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        if (countryCode != null) {
            adapter.addFragment(fragments[IDX_LOCAL], R.string.action_local);
        }
        adapter.addFragment(fragments[IDX_TOP_CLICK], R.string.action_top_click);
        adapter.addFragment(fragments[IDX_TOP_VOTE], R.string.action_top_vote);
        adapter.addFragment(fragments[IDX_CHANGED_LATELY], R.string.action_changed_lately);
        adapter.addFragment(fragments[IDX_CURRENTLY_HEARD], R.string.action_currently_playing);
        adapter.addFragment(fragments[IDX_TAGS], R.string.action_tags);
        adapter.addFragment(fragments[IDX_COUNTRIES], R.string.action_countries);
        adapter.addFragment(fragments[IDX_LANGUAGES], R.string.action_languages);
        adapter.addFragment(fragments[IDX_SEARCH], R.string.action_search);
        viewPager.setAdapter(adapter);
    }

    private void setupQingtingTabs(ViewPager viewPager) {
        fragments = new Fragment[4];

        // 推荐（使用 MODE_FEATURED 调用 getFeaturedStations）
        FragmentQingtingStations featuredFrag = new FragmentQingtingStations();
        Bundle featuredArgs = new Bundle();
        featuredArgs.putInt(FragmentQingtingStations.KEY_MODE, FragmentQingtingStations.MODE_FEATURED);
        featuredFrag.setArguments(featuredArgs);
        fragments[QT_IDX_FEATURED] = featuredFrag;

        // 地区
        FragmentQingtingCategories regionsFrag = new FragmentQingtingCategories();
        Bundle regionsArgs = new Bundle();
        regionsArgs.putString(FragmentQingtingCategories.KEY_CATEGORY_TYPE, FragmentQingtingCategories.TYPE_REGIONS);
        regionsFrag.setArguments(regionsArgs);
        fragments[QT_IDX_REGIONS] = regionsFrag;

        // 分类
        FragmentQingtingCategories classesFrag = new FragmentQingtingCategories();
        Bundle classesArgs = new Bundle();
        classesArgs.putString(FragmentQingtingCategories.KEY_CATEGORY_TYPE, FragmentQingtingCategories.TYPE_CLASSES);
        classesFrag.setArguments(classesArgs);
        fragments[QT_IDX_CLASSES] = classesFrag;

        // 搜索（使用 MODE_SEARCH + IFragmentSearchable，调用蜻蜓 API 搜索）
        FragmentQingtingStations searchFrag = new FragmentQingtingStations();
        Bundle searchArgs = new Bundle();
        searchArgs.putInt(FragmentQingtingStations.KEY_MODE, FragmentQingtingStations.MODE_SEARCH);
        searchFrag.setArguments(searchArgs);
        fragments[QT_IDX_SEARCH] = searchFrag;

        FragmentManager m = getChildFragmentManager();
        ViewPagerAdapter adapter = new ViewPagerAdapter(m);
        adapter.addFragment(fragments[QT_IDX_FEATURED], R.string.action_featured);
        adapter.addFragment(fragments[QT_IDX_REGIONS], R.string.action_regions);
        adapter.addFragment(fragments[QT_IDX_CLASSES], R.string.action_qingting_categories);
        adapter.addFragment(fragments[QT_IDX_SEARCH], R.string.action_search);
        viewPager.setAdapter(adapter);
    }

    private void setupViewPager(ViewPager viewPager) {
        if (isQingtingSource()) {
            setupQingtingTabs(viewPager);
        } else {
            setupRadioBrowserTabs(viewPager);
        }
    }

    public void Search(StationsFilter.SearchStyle searchStyle, final String query) {
        Log.d("TABS", "Search = " + query + " searchStyle=" + searchStyle);
        if (viewPager != null) {
            Log.d("TABS", "a Search = " + query);
            int searchIdx = isQingtingSource() ? QT_IDX_SEARCH : IDX_SEARCH;
            viewPager.setCurrentItem(searchIdx, false);
            ((IFragmentSearchable) fragments[searchIdx]).Search(searchStyle, query);
        } else {
            Log.d("TABS", "b Search = " + query);
            queuedSearchQuery = query;
            queuedSearchStyle = searchStyle;
        }
    }

    @Override
    public void Refresh() {
        int idx = viewPager.getCurrentItem();
        if (idx >= 0 && idx < fragments.length) {
            Fragment fragment = fragments[idx];
            if (fragment instanceof FragmentBase) {
                ((FragmentBase) fragment).DownloadUrl(true);
            }
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<Integer> mFragmentTitleList = new ArrayList<Integer>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, int title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Resources res = getResources();
            return res.getString(mFragmentTitleList.get(position));
        }
    }
}
