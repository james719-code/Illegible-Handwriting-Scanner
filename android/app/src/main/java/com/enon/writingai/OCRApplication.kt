package com.enon.writingai

import android.app.Application
import com.enon.writingai.core.di.AppContainer

class OCRApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
