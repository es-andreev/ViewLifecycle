package com.ea.viewlifecycle

import android.os.Bundle
import android.view.View
import kotlin.reflect.KProperty

internal class FragmentHolderDelegate : HolderDelegate<View, Bundle>() {

    override operator fun getValue(thisRef: View, property: KProperty<*>): Bundle? {
        return values[thisRef] ?: ViewCompanionFragment.get(thisRef)?.arguments
    }

    override operator fun setValue(thisRef: View, property: KProperty<*>, value: Bundle?) {
        super.setValue(thisRef, property, value)

        thisRef.onAttached {
            ViewCompanionFragment.getOrCreate(this).arguments = value
        }
    }
}