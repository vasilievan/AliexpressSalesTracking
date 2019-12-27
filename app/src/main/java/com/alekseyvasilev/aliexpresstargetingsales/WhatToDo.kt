package com.alekseyvasilev.aliexpresstargetingsales

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WhatToDo: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.what_to_do)
    }
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}