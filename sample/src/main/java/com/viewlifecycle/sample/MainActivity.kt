package com.viewlifecycle.sample

import android.app.Activity
import android.content.ContextWrapper
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.viewlifecycle.BackStackNavigator
import com.viewlifecycle.Navigator
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigator = BackStackNavigator(mainContainer, savedInstanceState)
        if (savedInstanceState == null) {
            navigator.navigateForward(MainView(this))
        }
    }

    override fun onBackPressed() {
        if (!navigator.navigateBack()) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            return navigator.navigateBack()
        }
        return super.onOptionsItemSelected(item)
    }
}

val View.activity: AppCompatActivity
    get() {
        var c = context
        while (c !is Activity && c is ContextWrapper) {
            c = c.baseContext
        }

        return c as AppCompatActivity
    }

val View.navigator: Navigator
    get() = (activity as MainActivity).navigator