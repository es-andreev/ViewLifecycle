package com.ea.viewlifecycle.sample

import android.view.View

interface MainNavigator {

    fun navigateForward(view: View)

    fun navigateBack()
}