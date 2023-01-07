package com.uz.kitoblar.ui.components.list

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.*
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.LoadingItemBinding
import com.uz.kitoblar.ui.controllers.Model
import com.uz.kitoblar.ui.inflateBinding

open class BaseDiffCallback<T : Model> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem == newItem
    }
}

const val VERTICAL = RecyclerView.VERTICAL
const val HORIZONTAL = RecyclerView.HORIZONTAL

fun RecyclerView.setLinearLayoutManager(
    orientation: Int = VERTICAL,
    reverse: Boolean = false
): LinearLayoutManager {
    LinearLayoutManager(context, orientation, reverse).apply {
        layoutManager = this
        return this
    }
}

open class BaseAdapter<T : Model, A : ViewDataBinding>(
    private val layId: Int,
    var diffUtil: DiffUtil.ItemCallback<Model> = BaseDiffCallback()
) : ListAdapter<Model, BaseAdapter.BaseViewHolder<A>>(diffUtil) {

    private var cache: ArrayList<ViewDataBinding>? = null


    private var recyclerView: RecyclerView? = null
    var clickListener: ((binding: BaseViewHolder<A>) -> Unit)? = null
    var onLongClickListener: ((binding: BaseViewHolder<A>) -> Unit)? = null

    private val loadingId = loadingLayId.toString()

    var loading = false
        set(value) {
            if (field != value) {
                field = value
                val newList = ArrayList(currentList)
                newList.find { it.id == loadingId }?.let { newList.remove(it) }

                if (value) {
                    val model = Model().apply { id = loadingId }
                    newList.add(model)
                }
                submitList(newList)
            }
        }

    companion object {
        const val loadingLayId = R.layout.loading_item
    }

    fun getViewHolder(position: Int): BaseViewHolder<A>? {
        if (position == -1) return null
        return recyclerView?.findViewHolderForAdapterPosition(position) as BaseViewHolder<A>?
    }

    open fun getLayoutId(position: Int): Int {
        return if (getItem(position)?.id == loadingId) loadingLayId else layId
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    override fun onFailedToRecycleView(holder: BaseViewHolder<A>): Boolean {
        return true
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView.apply {
            setHasFixedSize(true)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun getViewHolder(id: String): BaseViewHolder<A>? {
        val index = currentList.indexOfFirst { it.id == id }
        return getViewHolder(index)
    }

    open fun bind(holder: BaseViewHolder<A>, model: T) {

    }

    open fun bind(holder: BaseViewHolder<A>, model: T, position: Int) {

    }

    open fun holderCreated(holder: BaseViewHolder<A>, itemType: Int) {

    }

    class BaseViewHolder<T : ViewDataBinding>(val binding: T) :
        RecyclerView.ViewHolder(binding.root)

    open fun createBinding(parent: ViewGroup, viewType: Int): A {
        return inflateBinding(parent, viewType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<A> {
        val binding = createBinding(parent, viewType)
        val holder = BaseViewHolder(binding)
        holder.itemView.apply {
            clickListener?.let { l -> setOnClickListener { l.invoke(holder) } }
            onLongClickListener?.let { l ->
                setOnLongClickListener {
                    l.invoke(holder)
                    return@setOnLongClickListener true
                }
            }
        }
        if (holder.binding !is LoadingItemBinding) {
            holderCreated(holder, viewType)
        }
        return holder
    }

    override fun onBindViewHolder(holder: BaseViewHolder<A>, position: Int) {
        if (position < currentList.size) {
            val model = getItem(position)
            if (model.id != loadingId) {
                if (holder.binding !is  LoadingItemBinding) {
                    bind(holder, model as T)
                    bind(holder,model,position)
                }
            }
        }
    }

    fun updateList(list: ArrayList<T>?) {
        if (list == null) {
            submitList(null)
            return
        }
        submitList(list.toMutableList() as List<T>)
        notifyDataSetChanged()
    }
}
