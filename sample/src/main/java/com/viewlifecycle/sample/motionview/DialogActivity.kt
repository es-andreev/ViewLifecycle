package com.viewlifecycle.sample.motionview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.viewlifecycle.sample.R

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dialog)
        setFinishOnTouchOutside(true)
    }
}