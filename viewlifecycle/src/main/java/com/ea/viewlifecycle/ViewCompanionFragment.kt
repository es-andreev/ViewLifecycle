package com.ea.viewlifecycle

import android.arch.lifecycle.ViewModel
import android.support.v4.app.Fragment
import android.view.View

/**
 * ViewCompanionFragment is used:
 * - to obtain [ViewModel]s for views and retain them across configuration changes.
 * - to access arguments.
 * Unique per pair [View.javaClass] + [View.getId].
 */
internal class ViewCompanionFragment : Fragment() {

    companion object {
        internal fun getOrCreate(view: View): ViewCompanionFragment {
            return get(view)
                    ?: ViewCompanionFragment().also {
                        view.activity.supportFragmentManager
                                .beginTransaction()
                                .add(it, view.companionFragmentTag)
                                .commitNowAllowingStateLoss()
                    }
        }

        internal fun get(view: View): ViewCompanionFragment? {
            return view.activity.supportFragmentManager
                    .findFragmentByTag(view.companionFragmentTag)
                    as? ViewCompanionFragment
        }
    }
}