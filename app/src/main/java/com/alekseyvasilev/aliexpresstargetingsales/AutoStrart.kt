package com.alekseyvasilev.aliexpresstargetingsales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class AutoStart: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val intent = Intent(context, ChangesChecker::class.java)
        context?.startService(intent)
    }
}