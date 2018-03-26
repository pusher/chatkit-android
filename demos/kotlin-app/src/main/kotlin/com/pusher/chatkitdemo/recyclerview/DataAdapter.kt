package com.pusher.chatkitdemo.recyclerview

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.extensions.LayoutContainer
import kotlin.properties.Delegates

fun <A> dataAdapterFor(
    layoutRes: Int,
    onBind: LayoutContainer.(A) -> Unit
) : DataAdapter<A> = SimpleDataAdapter(layoutRes, onBind)

fun <A> dataAdapterFor(
    block: DataAdapterContext<A>.() -> Unit
) : DataAdapter<A> = MultiDataAdapter(
    DataAdapterContextWithMap<A>().apply(block).adapterMap
)

sealed class DataAdapter<A> : RecyclerView.Adapter<DataViewHolder<A>>() {

    var data: List<A> by Delegates.observable(emptyList()) { _, _, _ ->
        notifyDataSetChanged()
    }

    @JvmOverloads
    fun insert(item: A, index: Int = 0) {
        data = data.subList(0, index) + item + data.subList(index, data.size)
    }

    operator fun plusAssign(item: A) =
        insert(item)
}

sealed class DataViewHolder<in A>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(data: A)
}

typealias ViewHolderFactory<A> = (A) -> DataViewHolder<A>

/**
 * Generalised [RecyclerView.Adapter] for lists of simple items using android extensions for the binding.
 */
private class SimpleDataAdapter<A>(
    @LayoutRes private val layoutRes: Int,
    private val onBind: LayoutContainer.(A) -> Unit
) : DataAdapter<A>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BindDataViewHolder(parent, layoutRes, onBind)

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: DataViewHolder<A>, position: Int) =
        holder.bind(data[position])

}

private class BindDataViewHolder<in A>(
    parent: ViewGroup,
    @LayoutRes layoutRes: Int,
    private val onBind: LayoutContainer.(A) -> Unit
) : DataViewHolder<A>(
    LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
), LayoutContainer {

    override val containerView: View = itemView

    override fun bind(data: A) {
        onBind(data)
    }

}

private class MultiDataAdapter<A>(
    private val adapterMap: Map<(A) -> Boolean, IndexedAdapter<A>>
) : DataAdapter<A>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder<A> =
        adapterMap.values.first { it.viewType == viewType }.adapter.invoke(parent)

    override fun getItemCount(): Int =
        data.size

    override fun onBindViewHolder(holder: DataViewHolder<A>, position: Int) =
        holder.bind(data[position])

    override fun getItemViewType(position: Int): Int =
        adapterMap.entries
            .first { (accepts, _) -> accepts(data[position]) }.value.viewType

}

private data class IndexedAdapter<in A>(val viewType: Int, val adapter: (parent: ViewGroup) -> DataViewHolder<A>)

sealed class DataAdapterContext<A> {
    abstract fun on(accept: (A) -> Boolean, @LayoutRes layoutRes: Int, onBind: LayoutContainer.(A) -> Unit)

    inline fun  <reified B : A> on(@LayoutRes layoutRes: Int, crossinline onBind: LayoutContainer.(B) -> Unit) =
        on({ it is B }, layoutRes) { onBind(it as B) }
}

private class DataAdapterContextWithMap<A> : DataAdapterContext<A>() {

    val adapterMap = mutableMapOf<(A) -> Boolean, IndexedAdapter<A>>()

    override fun on(accept: (A) -> Boolean, @LayoutRes layoutRes: Int, onBind: LayoutContainer.(A) -> Unit) {
        adapterMap += accept to IndexedAdapter(adapterMap.size) { parent -> BindDataViewHolder(parent, layoutRes, onBind) }
    }

}
