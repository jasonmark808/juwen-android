package cc.juwen.reader.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cc.juwen.reader.R
import cc.juwen.reader.data.FeedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = FeedRepository(applicationContext)
        val oldIds = repository.loadCached()?.stories?.filter { it.important }?.map { it.id }?.toSet().orEmpty()
        repository.refresh().fold(
            onSuccess = { snapshot ->
                val newImportant = snapshot.stories.filter { it.important && it.id !in oldIds }
                if (newImportant.isNotEmpty()) notifyImportant(newImportant.first().title, newImportant.size)
                Result.success()
            },
            onFailure = { Result.retry() },
        )
    }

    private fun notifyImportant(title: String, count: Int) {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel("important", "重大新闻", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val text = if (count == 1) title else "$title 等 $count 条"
        val notification = NotificationCompat.Builder(applicationContext, "important")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("聚闻 · 重大新闻")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(title.hashCode(), notification)
    }
}

