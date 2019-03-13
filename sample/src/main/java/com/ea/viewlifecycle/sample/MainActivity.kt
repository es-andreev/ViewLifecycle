package com.ea.viewlifecycle.sample

import android.app.Activity
import android.content.ContextWrapper
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.ea.viewlifecycle.trackNavigation
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            mainContainer.trackNavigation()
            mainContainer.addView(MainView(this))
        }
    }

    override fun onBackPressed() {
        if (mainContainer.childCount > 1) {
            navigateBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun navigateForward(view: View) {
        mainContainer.addView(view)
    }

    override fun navigateBack() {
        if (mainContainer.childCount > 1) {
            mainContainer.removeViewAt(mainContainer.childCount - 1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            navigateBack()
            return true
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

val View.navigator: MainNavigator
    get() = activity as MainActivity