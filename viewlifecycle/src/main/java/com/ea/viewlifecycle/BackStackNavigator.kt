package com.ea.viewlifecycle

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import java.util.*

/**
 * Implements navigation within a [viewGroup] and manages its back stack.
 * Provide [Bundle] in a constructor to restore navigation state and back stack across
 * configuration changes.
 */
class BackStackNavigator(private val viewGroup: ViewGroup,
                         savedInstanceState: Bundle?) : Navigator {

    private var backStackItems = BackStack()

    val backStackItemsCount: Int
        get() = backStackItems.size

    private val activity = viewGroup.activity

    private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
        }

        override fun onActivityResumed(activity: Activity?) {
        }

        override fun onActivityStarted(activity: Activity?) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (this@BackStackNavigator.activity === activity) {
                val app = activity.applicationContext as? Application
                        ?: throw IllegalStateException()
                app.unregisterActivityLifecycleCallbacks(this)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            if (this@BackStackNavigator.activity === activity) {
                saveState(outState)
            }
        }

        override fun onActivityStopped(activity: Activity?) {
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }
    }

    init {
        val app = viewGroup.context.applicationContext as? Application
                ?: throw IllegalStateException()
        app.registerActivityLifecycleCallbacks(activityCallbacks)

        NavViewCompanionFragment.getOrCreate(viewGroup)

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        }
    }

    private fun saveState(bundle: Bundle) {
        bundle.putParcelable(STATE_ITEMS, backStackItems)
    }

    private fun restoreState(bundle: Bundle) {
        @Suppress("UNCHECKED_CAST")
        backStackItems = bundle.getParcelable(STATE_ITEMS) as? BackStack
                ?: BackStack()
    }

    override fun navigateForward(view: View) {
        val lastView = viewGroup.getChildAt(viewGroup.childCount - 1)
        lastView?.apply {
            backStackItems.add(BackStackItem.of(lastView))
            lastView.isBackStackItem = true
            viewGroup.removeView(lastView)
        }

        viewGroup.addView(view)
    }

    override fun navigateAdd(view: View) {
        val lastView = viewGroup.getChildAt(viewGroup.childCount - 1)
        lastView?.apply {
            val item = BackStackItem.of(lastView).apply {
                synchronizedWithView = false
            }
            lastView.isBackStackItem = true
            backStackItems.add(item)
        }

        viewGroup.addView(view)
    }

    override fun navigateBack(): Boolean {
        val hasBackStack = backStackItemsCount > 0
        if (hasBackStack && viewGroup.childCount > 0) {
            viewGroup.removeViewAt(viewGroup.childCount - 1)
        } else {
            return false
        }

        return restoreLastItem()
    }

    override fun navigateBackTo(className: String): Boolean {
        backStackItems.find {
            it.name == className
        } ?: return false

        if (viewGroup.childCount > 0) {
            viewGroup.removeViewAt(viewGroup.childCount - 1)
        } else {
            return false
        }

        while (backStackItems.isNotEmpty() && backStackItems.last().name != className) {
            backStackItems.removeLast()?.apply {
                removeCompanionFragment(this)
            }
        }

        return restoreLastItem()
    }

    override fun navigateBackIncluding(className: String): Boolean {
        val backStackTargetIndex = backStackItems.indexOfLast {
            it.name == className
        }
        if (backStackTargetIndex < 1) {
            return false
        }

        if (viewGroup.childCount > 0) {
            viewGroup.removeViewAt(viewGroup.childCount - 1)
        } else {
            return false
        }

        while (backStackItems.isNotEmpty() && backStackItems.last().name != className) {
            backStackItems.removeLast()?.apply {
                removeCompanionFragment(this)
            }
        }
        backStackItems.removeLast()?.apply {
            removeCompanionFragment(this)
        }

        return restoreLastItem()
    }

    override fun navigateReplace(view: View) {
        viewGroup.addView(view)
        if (viewGroup.childCount > 1) {
            viewGroup.removeViewAt(viewGroup.childCount - 2)
        }
    }

    override fun navigateReplaceAll(view: View) {
        viewGroup.removeAllViews()
        viewGroup.addView(view)

        while (backStackItems.isNotEmpty()) {
            backStackItems.removeLast()?.apply {
                removeCompanionFragment(this)
            }
        }
    }

    private fun restoreLastItem(): Boolean {
        val hasItems = backStackItems.isNotEmpty()
        if (hasItems) {
            val previousItem = backStackItems.removeLast()
            if (previousItem?.synchronizedWithView == true) {
                val viewClass = Class.forName(previousItem.name)
                val view = viewClass.getConstructor(Context::class.java)
                        .newInstance(viewGroup.context) as View

                view.restoreHierarchyState(previousItem.state)
                viewGroup.addView(view)
            }
        }
        return hasItems
    }

    private fun removeCompanionFragment(item: BackStackItem) {
        val companionFragment = activity.supportFragmentManager
                .findFragmentByTag(item.companionFragmentTag) as? ViewCompanionFragment

        if (companionFragment != null) {
            activity.supportFragmentManager
                    .beginTransaction()
                    .remove(companionFragment)
                    .commitAllowingStateLoss()
        }
    }

    private data class BackStackItem(
            val name: String,
            val state: SparseArray<Parcelable>,
            val companionFragmentTag: String) : Parcelable {

        var synchronizedWithView: Boolean = true

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
        constructor(parcel: Parcel) : this(
                parcel.readString(),
                parcel.readSparseArray(BackStackItem::class.java.classLoader)
                        as SparseArray<Parcelable>,
                parcel.readString()) {
            synchronizedWithView = parcel.readByte() != 0.toByte()
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            @Suppress("UNCHECKED_CAST")
            parcel.writeSparseArray(state as SparseArray<Any>)
            parcel.writeString(companionFragmentTag)
            parcel.writeByte(if (synchronizedWithView) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<BackStackItem> {
            override fun createFromParcel(parcel: Parcel): BackStackItem {
                return BackStackItem(parcel)
            }

            override fun newArray(size: Int): Array<BackStackItem?> {
                return arrayOfNulls(size)
            }

            fun of(view: View): BackStackItem {
                val className = view::class.java.canonicalName
                        ?: throw IllegalStateException()
                val stateContainer = SparseArray<Parcelable>()
                view.saveHierarchyState(stateContainer)

                return BackStackItem(className, stateContainer, view.companionFragmentTag)
            }
        }
    }

    private class BackStack() : Collection<BackStackItem>, Parcelable {

        private val impl = LinkedList<BackStackItem>()

        override val size: Int
            get() = impl.size

        constructor(parcel: Parcel) : this() {
            val array = parcel.createTypedArray(BackStackItem.CREATOR)
            array?.apply {
                impl.addAll(this)
            }
        }

        override fun iterator(): Iterator<BackStackItem> {
            return impl.iterator()
        }

        fun add(item: BackStackItem) {
            impl.add(item)
        }

        fun removeLast(): BackStackItem? {
            return impl.removeLast()
        }

        override fun contains(element: BackStackItem) = impl.contains(element)

        override fun containsAll(elements: Collection<BackStackItem>) = impl.containsAll(elements)

        override fun isEmpty() = impl.isEmpty()

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeTypedArray(impl.toTypedArray(), 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<BackStack> {
            override fun createFromParcel(parcel: Parcel): BackStack {
                return BackStack(parcel)
            }

            override fun newArray(size: Int): Array<BackStack?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val STATE_ITEMS = "STATE_ITEMS"
    }
}