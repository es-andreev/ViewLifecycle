package com.ea.viewlifecycle.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)
        setFinishOnTouchOutside(true)
    }
}