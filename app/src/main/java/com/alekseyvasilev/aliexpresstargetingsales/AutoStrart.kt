package com.alekseyvasilev.aliexpresstargetingsales

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*


class AutoStart: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val alarmUp = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, AutoStart::class.java),
            PendingIntent.FLAG_NO_CREATE
        ) != null
        if ((intent!!.action == "android.intent.action.BOOT_COMPLETED") && (!alarmUp)) {
            val alarmMgr: AlarmManager?
            lateinit var alarmIntent: PendingIntent
            alarmMgr = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmIntent = Intent(context, AutoStart::class.java).let { intent ->
                PendingIntent.getBroadcast(context, 0, intent, 0)
            }
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, this.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, this.get(Calendar.MINUTE))
            }
            alarmMgr.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                1000 * 60 * 5,
                alarmIntent
            )
        }
        val openCC = Intent(context, ChangesChecker::class.java)
        context?.startService(openCC)
    }
}