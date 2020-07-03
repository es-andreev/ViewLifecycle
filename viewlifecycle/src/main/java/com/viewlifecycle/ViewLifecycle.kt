package com.viewlifecycle

import android.content.ContextWrapper
import android.graphics.Region
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

/**
 * Obtain a [LifecycleOwner] of the view. View's lifecycle depends on:
 * - the lifecycle of the owning activity
 * - the lifecycle of the parent ViewGroup
 * - the layout position in the parent ViewGroup
 */
@Suppress("unused")
var View.lifecycleOwner: LifecycleOwner by ViewLifecycleOwnerDelegate {
    ensureParentLifecycleDispatcherAttached()
    attachLifecycleOwner()
}
    internal set

private fun View.ensureParentLifecycleDispatcherAttached() {
    onAttached {
        (parent as? ViewGroup)?.attachViewGroupLifecycleDispatcher()
    }
}

private fun View.attachLifecycleOwner(): LifecycleOwner {
    val current = rawLifecycleOwner
    if (current != null) {
        return current
    }

    // Special case when navigate methods are called multiple times (i.e. to form a back stack).
    // View is created, added and immediately removed in a ViewGroup,
    // not being attached to a window, so we need to quickly clear memory associated with it.
    if (!ViewCompat.isAttachedToWindow(this)) {
        mainHandler.postDelayed({
            if (!ViewCompat.isAttachedToWindow(this)) {
                destroy()
            }
        }, 100)
    }

    return ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}

internal fun View.detachLifecycleOwner() {
    lifecycleOwner = ViewLifecycleOwnerDelegate.NullLifecycleOwner
    rawLifecycleOwner = null
}

internal fun ViewGroup.attachViewGroupLifecycleDispatcher() {
    if (viewGroupLifecycleDispatcher != null) {
        return
    }

    attachLifecycleOwner()
    viewGroupLifecycleDispatcher = ViewGroupLifecycleDispatcher(this)
}

internal fun ViewGroup.detachViewGroupLifecycleDispatcher() {
    val dispatcher = viewGroupLifecycleDispatcher
    if (dispatcher != null) {
        dispatcher.clear()
        viewGroupLifecycleDispatcher = null
    }
}

internal fun ViewGroup.attachHierarchyLifecycleDispatcher() {
    if (hierarchyLifecycleDispatcher != null) {
        return
    }

    attachLifecycleOwner()
    hierarchyLifecycleDispatcher = HierarchyLifecycleDispatcher(this)
}

internal fun ViewGroup.detachHierarchyLifecycleDispatcher() {
    val dispatcher = hierarchyLifecycleDispatcher
    if (dispatcher != null) {
        dispatcher.clear()
        hierarchyLifecycleDispatcher = null
    }
}

@MainThread
inline fun <reified VM : ViewModel> View.viewModels(
        noinline viewModelScope: () -> View = { this },
        noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
) = ViewCompanionFragment.getOrCreate(this).viewModels<VM>(
        { ViewCompanionFragment.getOrCreate(viewModelScope()) },
        factoryProducer)

/**
 * Access arguments associated with a View.
 * The arguments are retained across configuration changes.
 */
@Suppress("unused")
var View.arguments: Bundle? by FragmentHolderDelegate()

internal fun View.destroy() {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).destroy()
        }
    }

    rawLifecycleOwner?.lifecycle?.currentState = Lifecycle.State.DESTROYED
}

internal val View.safeActivity: FragmentActivity?
    get() {
        var c = context
        while (c !is FragmentActivity && c is ContextWrapper) {
            c = c.baseContext
        }

        return c as? FragmentActivity
    }

internal val View.activity: FragmentActivity
    get() = safeActivity
            ?: throw IllegalStateException("Could not find FragmentActivity for $this.")

internal val View.safeRoot: ViewGroup?
    get() {
        var p = if (this is ViewGroup && parent != null) {
            this
        } else {
            parent as? ViewGroup
        }
        while (p?.parent is ViewGroup && (p.parent as View).safeActivity != null) {
            p = p.parent as ViewGroup
        }
        return p
    }

internal var View.rawLifecycleOwner: ViewLifecycleOwner? by HolderDelegate()

internal var ViewGroup.viewGroupLifecycleDispatcher: ViewGroupLifecycleDispatcher?
        by DispatcherHolderDelegate()

internal var View.hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? by HolderDelegate()

internal var View.level: Int
    get() = getTag(R.id.level) as? Int ?: 0
    set(value) = setTag(R.id.level, value)

internal var View.visibleRegion: Region?
    get() = getTag(R.id.region) as? Region
    set(value) = setTag(R.id.region, value)

internal var View.isBackStackItem: Boolean
    get() = getTag(R.id.backStackItem) == true
    set(value) = setTag(R.id.backStackItem, value)

internal fun View.updateState(state: Lifecycle.State) {
    if (!state.isAtLeast(Lifecycle.State.STARTED) || level == 0 && isDisplayed) {
        rawLifecycleOwner?.lifecycle?.currentState = state
    } else {
        rawLifecycleOwner?.lifecycle?.currentState = Lifecycle.State.CREATED
    }
}

internal val View.isDisplayed: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility != View.GONE

internal val View.innerStem: ArrayList<ViewGroup>
    get() {
        val parents = ArrayList<ViewGroup>()
        (this as? ViewGroup)?.apply {
            parents.add(this)
        }
        var p: ViewGroup? = parent as? ViewGroup
        val r = safeRoot
        while (p is ViewGroup) {
            if (p === r) {
                break
            }
            parents.add(p)
            p = p.parent as? ViewGroup
        }
        return parents
    }

internal val View.fullStem: ArrayList<ViewGroup>
    get() {
        val parents = ArrayList<ViewGroup>()
        parents.addAll(innerStem)
        safeRoot?.apply {
            parents.add(this)
        }
        return parents
    }

internal val View.navCompanionFragmentTag: String
    get() = "NavViewCompanionFragment for ${javaClass.canonicalName} : $id"

internal val View.companionFragmentTag: String
    get() = "ViewCompanionFragment for ${javaClass.canonicalName} : $id"

internal inline fun <T : View> T.onAttached(crossinline callback: T.() -> Unit) {
    if (ViewCompat.isAttachedToWindow(this)) {
        callback()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                removeOnAttachStateChangeListener(this)
            }

            override fun onViewAttachedToWindow(v: View?) {
                removeOnAttachStateChangeListener(this)

                callback()
            }
        })
    }
}

private val mainHandler = Handler(Looper.getMainLooper())
