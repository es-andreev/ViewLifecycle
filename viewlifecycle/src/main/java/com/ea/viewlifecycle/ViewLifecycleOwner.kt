package com.ea.viewlifecycle

import android.arch.lifecycle.LifecycleOwner
import android.view.View

/**
 * ViewLifecycleOwner is a special [LifecycleOwner] that is owned by a [View].
 * You can obtain it through the extension property [View.lifecycleOwner].
 */
internal class ViewLifecycleOwner(view: View) : LifecycleOwner {

    private val lifecycleRegistry = ViewLifecycleRegistry.create(this, view)

    override fun getLifecycle(): ViewLifecycleRegistry = lifecycleRegistry
}