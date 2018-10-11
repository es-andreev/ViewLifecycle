package com.ea.viewlifecycleowner

import android.arch.lifecycle.LifecycleOwner
import android.view.View
import android.view.ViewGroup

/**
 * ViewLifecycleOwner is a special [LifecycleOwner] that is owned by a [View].
 * It's bound to a view by mean of a weak access, see [LazyLifecycleOwnerDelegate].
 *
 * You can obtain it through the extension property [View.lifecycleOwner].
 * Once obtained, it will live no longer than a view's owning activity.
 * Notice however, that if you are handling views dynamically, i.e. adding and removing them
 * in a ViewGroup, you *must* destroy the owning View manually when you no longer need it
 * by calling [View.destroy] or one of [ViewGroup.removeAndDestroyView] extension functions.
 * By not destroying a View with a [ViewLifecycleOwner] or [ViewCompanionFragment] attached,
 * you get a memory leak.
 * If a navigation is attached to a ViewGroup by [ViewGroup.attachNavigation],
 * you can remove its direct children regularly, the library will destroy them for you.
 */
internal class ViewLifecycleOwner(view: View) : LifecycleOwner {

    private val lifecycleRegistry = ViewLifecycleRegistry(this, view)

    override fun getLifecycle(): ViewLifecycleRegistry = lifecycleRegistry
}