package com.alekseyvasilev.aliexpresstargetingsales

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val db = DataBase(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(applicationContext, ChangesChecker::class.java)
        applicationContext.startService(intent)

        val cursor = db.writableDatabase.query(db.TABLE_NAME, null, null, null, null, null, null, null)
        val layout = findViewById<LinearLayout>(R.id.layout)
        if (cursor.moveToFirst()) {
            do {
                val textToAdd = TextView(getApplicationContext())
                textToAdd.setTextSize(20f)
                textToAdd.setPadding(20, 20, 20, 20)
                textToAdd.gravity = 1
                textToAdd.setId(cursor.getInt(cursor.getColumnIndex(db.KEY_ID)))
                textToAdd.setText(cursor.getString(cursor.getColumnIndex(db.KEY_GOOD_NAME)) + " " + cursor.getString(cursor.getColumnIndex(db.KEY_PRICE)) + " units")
                textToAdd.setOnLongClickListener {
                    layout.removeView(textToAdd)
                    db.writableDatabase.delete(db.TABLE_NAME, db.KEY_ID + " = " + textToAdd.id, null)
                    Toast.makeText(getApplicationContext(), "A good was deleted.", Toast.LENGTH_LONG).show()
                    return@setOnLongClickListener true
                }
                textToAdd.setOnClickListener{
                    val cursor = db.writableDatabase.query(db.TABLE_NAME, null, null, null, null, null, null, null)
                    if (cursor.moveToFirst()) {
                        do {
                            if (textToAdd.id == cursor.getInt(cursor.getColumnIndex(db.KEY_ID))) {
                                val openLink =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(cursor.getString(cursor.getColumnIndex(db.KEY_LINK))))
                                startActivity(openLink)
                                break
                            }
                        } while (cursor.moveToNext())
                    }
                }
                layout.addView(textToAdd)
            } while (cursor.moveToNext())
        } else {
            Toast.makeText(this, "No goods found.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    fun netIsAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if ((cm.activeNetworkInfo != null) && cm.activeNetworkInfo.isConnected) {
            return true
        }
        return false
    }

    private fun alreadyInDataBase(link: String): Boolean {
        val cursor = db.writableDatabase.query(db.TABLE_NAME, null, null, null, null, null, null, null)
        if (cursor.moveToFirst()) {
            do {
                if ((cursor.getString(cursor.getColumnIndex(db.KEY_LINK)) == link) || (cursor.getString(cursor.getColumnIndex(db.KEY_LINK)) == link.replaceFirst("m.", ""))) {
                    return true
                }
            } while (cursor.moveToNext())
            return false
        } else {
            return false
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

    fun onQClick(view: View) {
        val intent = Intent(this, WhatToDo::class.java)
        startActivity(intent)
    }

    fun onWriteClick(view: View){
        if (!netIsAvailable()) {
            Toast.makeText(this, "Net is unavailable. Try again later.", Toast.LENGTH_LONG).show()
            return Unit
        }
        val inputLink = findViewById<EditText>(R.id.link)
        val inputLinkText = inputLink.getText().toString()
        if (inputLinkText.isEmpty()) {
            Toast.makeText(this, "Please, print the text.", Toast.LENGTH_SHORT).show()
        } else if (!inputLinkText.matches(Regex("""https:\/\/.*aliexpress\..+\/.+"""))) {
            inputLink.setText("")
            Toast.makeText(this, "Incorrect input.", Toast.LENGTH_SHORT).show()
        } else {
            if (alreadyInDataBase(inputLinkText)) {
                Toast.makeText(this, "Error. Already presented in the database.", Toast.LENGTH_LONG).show()
                inputLink.setText("")
                return Unit
            }
            var temp = listOf("", "")
            class MyTask: AsyncTask<Any?, Any?, Any?>() {
                override fun doInBackground(vararg params: Any?) {
                    if (inputLinkText.startsWith("https://m.")) {
                        temp = connection(inputLinkText.replaceFirst("m.", ""))
                    } else {
                        temp = connection(inputLinkText)
                    }
                }
                override fun onPostExecute(result: Any?) {
                    super.onPostExecute(result)

                    val myDb = db.writableDatabase
                    val contentValues = ContentValues()
                    contentValues.put(db.KEY_LINK, inputLinkText)
                    contentValues.put(db.KEY_GOOD_NAME, temp[0])
                    contentValues.put(db.KEY_PRICE, temp[1])
                    myDb.insert(db.TABLE_NAME, null, contentValues)

                    val layout = findViewById<LinearLayout>(R.id.layout)
                    val textToAdd = TextView(getApplicationContext())
                    textToAdd.setTextSize(20f)
                    textToAdd.setPadding(20, 20, 20, 20)
                    textToAdd.gravity = 1

                    val cursor = db.writableDatabase.query(db.TABLE_NAME, null, null, null, null, null, null, null)
                    cursor.moveToLast()
                    textToAdd.setId(cursor.getInt(cursor.getColumnIndex(db.KEY_ID)))
                    textToAdd.setText(temp[0] + " " + temp[1] + " units")
                    textToAdd.setOnLongClickListener {
                        layout.removeView(textToAdd)
                        db.writableDatabase.delete(db.TABLE_NAME, db.KEY_ID + " = " + textToAdd.id, null)
                        Toast.makeText(getApplicationContext(), "A good was deleted.", Toast.LENGTH_LONG).show()
                        return@setOnLongClickListener true
                    }

                    textToAdd.setOnClickListener{
                        val cursor = db.writableDatabase.query(db.TABLE_NAME, null, null, null, null, null, null, null)
                        if (cursor.moveToFirst()) {
                            do {
                                if (textToAdd.id == cursor.getInt(cursor.getColumnIndex(db.KEY_ID))) {
                                    val openLink =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(cursor.getString(cursor.getColumnIndex(db.KEY_LINK))))
                                    startActivity(openLink)
                                    break
                                }
                            } while (cursor.moveToNext())
                        }
                    }
                    layout.addView(textToAdd)

                    Toast.makeText(getApplicationContext(), temp[0] + " that costs " + temp[1] + " was added to waiting list.", Toast.LENGTH_LONG).show()
                }
            }
            val mt = MyTask()
            mt.execute()
        }
        inputLink.setText("")
    }
}
