package com.onedongua.radiodrive.interfaces;

import com.onedongua.radiodrive.station.StationsFilter;

public interface IFragmentSearchable {
    void Search(StationsFilter.SearchStyle searchStyle, String query);
}
