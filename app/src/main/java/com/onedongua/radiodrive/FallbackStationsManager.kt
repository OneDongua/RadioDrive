package com.onedongua.radiodrive

import android.content.Context
import com.onedongua.radiodrive.station.DataRadioStation

class FallbackStationsManager(ctx: Context?) : StationSaveManager(ctx) {
    override fun Load() {
        listStations.clear()
        val str = context.resources
            .openRawResource(R.raw.fallback_stations)
            .bufferedReader()
            .use { it.readText() }
        val arr = DataRadioStation.DecodeJson(str)
        listStations.addAll(arr)
    }
}
