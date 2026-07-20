package com.onedongua.radiodrive;

import android.content.Context;

import com.onedongua.radiodrive.source.RadioDataSourceManager;
import com.onedongua.radiodrive.station.DataRadioStation;

public class HistoryManager extends StationSaveManager {
    private static final int MAXSIZE = 25;

    @Override
    protected String getSaveId() {
        try {
            RadioDataSourceManager mgr = RadioDataSourceManager.getInstance();
            return mgr.getCurrentSource().getSourceType() + "_history";
        } catch (Exception e) {
            return "history";
        }
    }

    public HistoryManager(Context ctx) {
        super(ctx);
    }

    @Override
    public void add(DataRadioStation station) {
        DataRadioStation stationFromHistory = getById(station.StationUuid);
        if (stationFromHistory != null) {
            listStations.remove(stationFromHistory);
            listStations.add(0, stationFromHistory);
            Save();
            return;
        }

        cutList(MAXSIZE - 1);
        super.addFront(station);
    }

    private void cutList(int count) {
        if (listStations.size() > count) {
            listStations = listStations.subList(0, count);
        }
    }
}
