package com.ea.viewlifecycleowner.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class ActivityDialog : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)
        setFinishOnTouchOutside(true)
    }
}