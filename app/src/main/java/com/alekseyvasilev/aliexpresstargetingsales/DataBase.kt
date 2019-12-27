package com.alekseyvasilev.aliexpresstargetingsales

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBase(
    context: Context,
    name: String = "myData",
    version: Int = 1
) : SQLiteOpenHelper(context, name, null, version) {

    val TABLE_NAME = "LinksPrices"
    val KEY_ID = "_id"
    val KEY_LINK = "link"
    val KEY_PRICE = "price"
    val KEY_GOOD_NAME = "name"

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL("create table " + TABLE_NAME + " ( " + KEY_ID + " integer primary key, " + KEY_LINK + " text, " + KEY_GOOD_NAME + " text, " + KEY_PRICE + " real" + " );")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("drop table if exists " + TABLE_NAME)
        onCreate(db)
    }
}