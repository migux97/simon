package com.migux97.simon

import android.app.Application
import com.migux97.simon.data.Repository
import com.migux97.simon.widget.RefreshWorker
import com.migux97.simon.widget.WidgetUpdater

class SimonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repository.init(this)
        RefreshWorker.schedule(this)
        WidgetUpdater.scheduleTick(this)
    }
}
