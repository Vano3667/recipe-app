package com.example.homemenu

import android.app.Application
import android.util.Log
import com.example.homemenu.data.seed.Seeder
import com.example.homemenu.di.AppScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HomeMenuApp : Application() {
    @Inject lateinit var seeder: Seeder
    @Inject @AppScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { seeder.seed() }.onFailure { Log.e("HomeMenu", "Seeding failed", it) }
        }
    }
}
