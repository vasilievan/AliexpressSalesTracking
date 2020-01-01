package com.alekseyvasilev.aliexpresstargetingsales

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.util.*

class ChangesChecker: Service() {
    val LOG_TAG = "myLog"
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    fun netIsAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if ((cm.activeNetworkInfo != null) && cm.activeNetworkInfo.isConnected) {
            return true
        }
        return false
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(name, name, importance).apply {
            description = name
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun notifyDearUser(title: String, str: String, link: String) {
        createNotificationChannel()

        val builder = NotificationCompat.Builder(this, getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_mipmap_mini)
            .setContentTitle(title)
            .setContentText(str)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (link != "nothing") {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
            builder.setContentIntent(pendingIntent)
            with(NotificationManagerCompat.from(this)) {
                notify((0..Int.MAX_VALUE).random(), builder.build())
            }
        } else {
            with(NotificationManagerCompat.from(this)) {
                notify((0..Int.MAX_VALUE).random(), builder.build())
            }
        }
    }

    private fun connection (url: String): List<String> {
        try {
            val doc: Document = Jsoup.connect(url)
                .userAgent("Chrome/4.0.249.0 Safari/532.5")
                .referrer("http://www.google.com")
                .get()
            val listOfScripts: Elements = doc.getElementsByTag("script")
            var stringWeAreLookingFor = ""
            for (element in listOfScripts) {
                for (dataNode in element.dataNodes()) {
                    val content = Regex("""var GaData = \{\n.+\n.+\n.+\n.+""").findAll(dataNode.wholeData)
                        .joinToString("") { it.value }
                    if (content.isNotEmpty()) {
                        stringWeAreLookingFor = content
                    }
                }
            }
            val answer = mutableListOf<String>()
            answer.add("${doc.getElementsByTag("title")[0].text().take(35)}...")
            answer.add(Regex("""totalValue: .*""").findAll(stringWeAreLookingFor).map { it.value }.joinToString { it }.replace(Regex("""[^\d,.]"""), "").replace(",", ".").removeSuffix("."))
            return answer
        } catch (e: IOException) {
        }
        return listOf()
    }

    override fun onDestroy() {
        val intent = Intent(this, ChangesChecker::class.java)
        startService(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        class MyTask: TimerTask() {
            override fun run() {
                val db = DataBase(applicationContext)
                val cursor = db.writableDatabase.query(
                    db.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
                val prices = mutableListOf<Double>()
                val names = mutableListOf<String>()
                val links = mutableListOf<String>()
                if (cursor.moveToFirst()) {
                    do {
                        prices.add(cursor.getDouble(cursor.getColumnIndex(db.KEY_PRICE)))
                        names.add(cursor.getString(cursor.getColumnIndex(db.KEY_GOOD_NAME)))
                        links.add(cursor.getString(cursor.getColumnIndex(db.KEY_LINK)))
                    } while (cursor.moveToNext())
                }
                for (element in 0 until links.size) {
                    if (netIsAvailable()) {
                        val price = connection("https://" + Regex("""aliexpress.+""").find(links[element])!!.value)[1].toDoubleOrNull()
                        if ((price != null) && (price != prices[element])) {
                            val cv = ContentValues()
                            cv.put(db.KEY_LINK, links[element])
                            cv.put(db.KEY_GOOD_NAME, names[element])
                            cv.put(db.KEY_PRICE, price)
                            val temp = db.KEY_LINK + " = " + "\"${links[element]}\""
                            db.writableDatabase.update(db.TABLE_NAME, cv, temp, null)
                            val signedValue = if (price.toDouble() / prices[element] * 100 - 100 > 0) {
                                "+" + (Math.round((price.toDouble() / prices[element] * 100 - 100) * 100)/ 100).toString()
                            } else {
                                "-" + (Math.round((100.0 - price.toDouble() / prices[element] * 100.0) * 100)/ 100).toString()
                            }
                            notifyDearUser(
                                "Revision",
                                names[element] + " now costs " + signedValue + "%. Click here to follow the link.",
                                links[element]
                            )
                        }
                    }
                }
                db.close()
            }
        }
        val timer = Timer()
        timer.schedule(MyTask(), 0,1000 * 2 * 60)
        return super.onStartCommand(intent, flags, startId)
    }
}