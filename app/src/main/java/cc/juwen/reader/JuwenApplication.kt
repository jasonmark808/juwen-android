package cc.juwen.reader

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cc.juwen.reader.sync.FeedSyncWorker
import java.util.concurrent.TimeUnit

class JuwenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequestBuilder<FeedSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "news-sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
