package com.example.wifihot

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport


class MainApplication : Application() {

    companion object {
        lateinit var application: Application
    }


    override fun onCreate() {
        super.onCreate()



        application = this
        CrashReport.initCrashReport(this, "a56b5010b6", false);
    }


}