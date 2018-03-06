package com.pusher.chatkitdemo.recyclerview

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlin.properties.Delegates

fun <A> dataAdapterFor(
    layoutRes: Int,
    onBind: LayoutContainer.(A) -> Unit
) = DataAdapter(layoutRes, onBind)

/**
 * Generalised [RecyclerView.Adapter] for lists of simple items using android extensions for the binding.
 */
class DataAdapter<A>(
    private val layoutRes: Int,
    private val onBind: LayoutContainer.(A) -> Unit
) : RecyclerView.Adapter<DataViewHolder<A>>() {

    var data: List<A> by Delegates.observable(emptyList()) { _, _, new ->
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DataViewHolder(parent, layoutRes, onBind)

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: DataViewHolder<A>, position: Int) =
        holder.bind(data[position])

}

class DataViewHolder<in A>(
    parent: ViewGroup,
    layoutRes: Int,
    private val onBind: LayoutContainer.(A) -> Unit
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
), LayoutContainer {

    override val containerView: View = itemView

    fun bind(data: A) {
        onBind(data)
    }

}
