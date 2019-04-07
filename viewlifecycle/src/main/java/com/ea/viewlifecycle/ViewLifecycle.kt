package com.ea.viewlifecycle

import android.arch.lifecycle.*
import android.content.ContextWrapper
import android.graphics.Region
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewCompat
import android.view.View
import android.view.ViewGroup
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

    return ViewLifecycleOwner(this).also {
        rawLifecycleOwner = it
    }
}

internal fun View.detachLifecycleOwner() {
    lifecycleOwner = ViewLifecycleOwnerDelegate.NullLifecycleOwner
    rawLifecycleOwner = null
}

/**
 * Mark a [ViewGroup] as a navigation container. After a configuration change,
 * if a ViewGroup with the same id is found in the hierarchy,
 * all its direct children will be restored.
 */
fun ViewGroup.trackNavigation(track: Boolean = true) {
    setTag(R.id.viewGroup_navigator, if (track) ViewGroupNavigator else null)

    if (track) {
        ensureParentLifecycleDispatcherAttached()

        // ensure ViewCompanionFragment is attached
        ViewCompanionFragment.getOrCreate(this)
    }
}

internal val ViewGroup.isTrackingNavigation: Boolean
    get() = getTag(R.id.viewGroup_navigator) === ViewGroupNavigator

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

/**
 * Obtain a [ViewModelProvider] associated with a View.
 */
@Suppress("unused")
val View.viewModelProvider: ViewModelProvider
    get() = viewModelProvider(null)

/**
 * Obtain a [ViewModelProvider] associated with a View.
 *
 * @param factory an optional ViewModelProvider.Factory for creating [ViewModel]s.
 */
fun View.viewModelProvider(factory: ViewModelProvider.Factory? = null): ViewModelProvider {
    val state = rawLifecycleOwner?.lifecycle?.currentState
    if (state?.isAtLeast(Lifecycle.State.CREATED) != true) {
        throw IllegalStateException("Cannot create ViewModelProvider until " +
                "LifecycleOwner is in created state.")
    }
    val companionFragment = ViewCompanionFragment.getOrCreate(this)
    return ViewModelProviders.of(companionFragment, factory)
}

/**
 * Access arguments associated with a View.
 * The arguments are retained across configuration changes.
 */
var View.arguments: Bundle? by HolderDelegate()

internal fun View.destroy() {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).destroy()
        }
    }

    rawLifecycleOwner?.lifecycle?.markState(Lifecycle.State.DESTROYED)
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

internal var View.viewGroupLifecycleDispatcher: ViewGroupLifecycleDispatcher?
        by DispatcherHolderDelegate()

internal var View.hierarchyLifecycleDispatcher: HierarchyLifecycleDispatcher? by HolderDelegate()

internal var View.level: Int
    get() = getTag(R.id.level) as? Int ?: 0
    set(value) = setTag(R.id.level, value)

internal var View.visibleRegion: Region?
    get() = getTag(R.id.region) as? Region
    set(value) = setTag(R.id.region, value)

internal fun View.updateState(state: Lifecycle.State) {
    if (!state.isAtLeast(Lifecycle.State.STARTED) || level == 0 && isDisplayed) {
        rawLifecycleOwner?.lifecycle?.markState(state)
    } else {
        rawLifecycleOwner?.lifecycle?.markState(Lifecycle.State.CREATED)
    }
}

internal val View.isDisplayed: Boolean
    get() = ViewCompat.isAttachedToWindow(this) && visibility != View.GONE

internal object ViewGroupNavigator

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