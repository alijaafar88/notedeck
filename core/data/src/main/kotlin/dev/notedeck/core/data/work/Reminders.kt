package dev.notedeck.core.data.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.notedeck.core.data.DeckDao
import dev.notedeck.domain.isDue
import javax.inject.Inject
import kotlin.time.Instant

/** How many cards are due now. An interface, so worker tests can pass a number. */
interface DueCardCounter {
    suspend fun countDue(now: Instant): Int
}

class RoomDueCardCounter @Inject constructor(private val dao: DeckDao) : DueCardCounter {
    override suspend fun countDue(now: Instant): Int = dao.schedules().count { isDue(it.box, it.lastReviewedAt, now) }
}

/** Everything Android-specific about the reminder, behind an interface the worker test can fake. */
interface ReminderNotifier {
    fun canNotify(): Boolean

    fun showReviewsDue(count: Int)
}

class AndroidReminderNotifier @Inject constructor(@ApplicationContext private val context: Context) : ReminderNotifier {
    /** Creating a channel that exists already does nothing, so this is safe at every start. */
    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Review reminders", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "A daily nudge when cards are due" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Android 13+ needs the runtime permission; without it, notify() would silently do nothing. */
    override fun canNotify(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun showReviewsDue(count: Int) {
        if (!canNotify()) return
        // The app's launch intent: the data layer opens the app without knowing its Activity.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val open = PendingIntent.getActivity(context, 0, launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (count == 1) "1 review due" else "$count reviews due")
            .setContentText("A few minutes now keeps them from piling up.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_ID, notification)
        } catch (e: SecurityException) {
            // The permission was revoked between the check and the call; skip this reminder.
        }
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        const val REMINDER_ID = 1
    }
}
