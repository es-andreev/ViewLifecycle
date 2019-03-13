package com.ea.viewlifecycle.sample.motionview

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ea.viewlifecycle.sample.R

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)
        setFinishOnTouchOutside(true)
    }
}