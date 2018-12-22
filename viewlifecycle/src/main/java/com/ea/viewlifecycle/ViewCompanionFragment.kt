package com.ea.viewlifecycle

import android.arch.lifecycle.ViewModel
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.view.View
import android.view.ViewGroup

/**
 * ViewCompanionFragment is a helper fragment and is bound to the owning View -
 * conversely to a fragments framework, where a View is an internal part of a Fragment.
 *
 * It's used:
 * - to access [ViewModel]s for views across configuration changes.
 * - to restore navigation into a [owningView], if such a view is found after configuration change.
 *
 * Unique per pair [View.javaClass] + [View.getId].
 */
class ViewCompanionFragment : Fragment() {

    private var owningViewId: Int = View.NO_ID
    private var owningView: View? = null
        set(value) {
            owningViewId = value?.id ?: View.NO_ID
            field = value
        }

    internal var destroyed: Boolean = false
        set(value) {
            if (value) {
                owningView = null
                activity?.supportFragmentManager?.beginTransaction()
                        ?.remove(this)?.commitAllowingStateLoss()
            }
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            destroyed = savedInstanceState.getBoolean(STATE_DESTROYED)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            owningViewId = savedInstanceState.getInt(STATE__VIEW_ID)
            if (owningViewId != View.NO_ID) {
                owningView = activity?.findViewById(owningViewId)
            }

            if (owningView == null) {
                destroyed = true
            } else if (owningView is ViewGroup) {
                val viewGroup = owningView as ViewGroup
                viewGroup.restoreStack(savedInstanceState)
            }
        }
    }

    override fun onDestroy() {
        owningView = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if ((owningView as? ViewGroup)?.isTrackingNavigation == true) {
            val viewGroup = owningView as ViewGroup
            viewGroup.saveStack(outState)
        }

        outState.putInt(STATE__VIEW_ID, owningViewId)
        outState.putBoolean(STATE_DESTROYED, destroyed)
        super.onSaveInstanceState(outState)
    }

    private fun ViewGroup.saveStack(bundle: Bundle) {
        val stackData = ArrayList<StackData>()
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            stackData.add(StackData.of(view))
        }
        bundle.putParcelableArrayList(STATE_STACK, stackData)
    }

    private fun ViewGroup.restoreStack(bundle: Bundle?) {
        val stack = bundle?.getParcelableArrayList<StackData>(STATE_STACK)
        if (stack != null) {
            stack.forEach {
                if (findViewById<View>(it.id) == null) {
                    val viewClass = Class.forName(it.viewClassName)
                    val view = viewClass.getConstructor(Context::class.java)
                            .newInstance(activity) as View
                    view.id = it.id
                    view.arguments = it.args

                    addView(view)
                }
            }
            trackNavigation()
        }
    }

    companion object {
        private const val STATE__VIEW_ID = "viewId"
        private const val STATE_DESTROYED = "destroyed"
        private const val STATE_STACK = "stack"

        internal fun get(view: View): ViewCompanionFragment? {
            return view.activity.supportFragmentManager.findFragmentByTag(view.companionFragmentTag)
                    as? ViewCompanionFragment
        }

        internal fun getOrCreate(view: View): ViewCompanionFragment {
            return get(view)
                    ?: ViewCompanionFragment().also {
                        if (view.id == View.NO_ID) {
                            if ((view.parent as? ViewGroup)?.isTrackingNavigation != true) {
                                // view must have a unique id at this time, because otherwise
                                // this ViewCompanionFragment won't be able to find it after
                                // configuration change and thus will be destroyed.
                                throw IllegalStateException("View must have an id.")
                            } else {
                                view.id = generateViewId()
                            }
                        }

                        it.owningView = view
                        view.activity.supportFragmentManager
                                .beginTransaction()
                                .add(it, view.companionFragmentTag)
                                .commitNowAllowingStateLoss()
                    }
        }

        private val View.companionFragmentTag: String
            get() = "Companion fragment for ${javaClass.canonicalName} : $id"
    }

    private data class StackData(val viewClassName: String, val id: Int, val args: Bundle?) : Parcelable {

        constructor(parcel: Parcel) : this(
                parcel.readString()
                        ?: throw NullPointerException("Error reading view class name from parcel."),
                parcel.readInt(),
                parcel.readBundle(Bundle::class.java.classLoader))

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(viewClassName)
            parcel.writeInt(id)
            parcel.writeBundle(args)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<StackData> {
            override fun createFromParcel(parcel: Parcel): StackData {
                return StackData(parcel)
            }

            override fun newArray(size: Int): Array<StackData?> {
                return arrayOfNulls(size)
            }

            fun of(view: View) = StackData(view::class.java.canonicalName
                    ?: throw RuntimeException("View must be a top level class."), view.id, view.arguments)
        }
    }
}