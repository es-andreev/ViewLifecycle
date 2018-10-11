package com.ea.viewlifecycleowner

import java.util.*
import kotlin.reflect.KProperty

internal class HolderDelegate<T> {

    private val values = WeakHashMap<Any, T?>()

    operator fun getValue(thisRef: Any, property: KProperty<*>): T? = synchronized(values) {
        return values[thisRef]
    }

    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        if (value == null) {
            values.remove(thisRef)
        } else {
            values[thisRef] = value
        }
    }
}