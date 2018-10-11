package com.ea.viewlifecycleowner

import android.support.v7.widget.RecyclerView
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DestroyableRecycledViewPool : RecyclerView.RecycledViewPool(), Destroyable {

    private val itemViewTypes = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    override fun putRecycledView(scrap: RecyclerView.ViewHolder) {
        val oldCount = getRecycledViewCount(scrap.itemViewType)

        super.putRecycledView(scrap)

        val newCount = getRecycledViewCount(scrap.itemViewType)
        if (newCount > oldCount) {
            itemViewTypes.add(scrap.itemViewType)
        }
    }

    override fun getRecycledView(viewType: Int): RecyclerView.ViewHolder? {
        val vh = super.getRecycledView(viewType)
        if (vh != null && getRecycledViewCount(vh.itemViewType) == 0) {
            itemViewTypes.remove(vh.itemViewType)
        }
        return vh
    }

    override fun destroy() {
        for (itemViewType in itemViewTypes) {
            for (i in 0 until getRecycledViewCount(itemViewType)) {
                val v = getRecycledView(itemViewType)
                v?.itemView?.destroy()
            }
        }
    }
}