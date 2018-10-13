package com.ea.viewlifecycle

import android.arch.lifecycle.LifecycleOwner
import android.view.View
import java.util.*
import kotlin.reflect.KProperty

internal class LazyLifecycleOwnerDelegate(private val initializer: View.() -> LifecycleOwner) {

    private val values = WeakHashMap<View, LifecycleOwner>()

    operator fun getValue(thisRef: View, property: KProperty<*>): LifecycleOwner = synchronized(values) {
        return values.getOrPut(thisRef) { initializer(thisRef) }
    }

    operator fun setValue(thisRef: View, property: KProperty<*>, value: LifecycleOwner?) {
        if (value == NullLifecycleOwner && values.containsKey(thisRef)) {
            values.remove(thisRef)
        } else if (value != null && !values.containsKey(thisRef)) {
            values[thisRef] = value
        } else if (value != null && values[thisRef] != null && values[thisRef] != value) {
            throw IllegalStateException("$thisRef is already attached to LifecycleOwner.")
        }
    }

    companion object NullLifecycleOwner : LifecycleOwner {
        override fun getLifecycle() = throw NullPointerException()
    }
}