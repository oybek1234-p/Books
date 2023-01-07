package com.uz.kitoblar.ui.components.actionBar

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.uz.kitoblar.ui.inflateBinding

open class SimpleAdapter<T : ViewDataBinding, M>(
    @LayoutRes val layoutId: Int,
    ) : RecyclerView.Adapter<BaseViewHolder<T>>() {

        var mRecyclerView: RecyclerView? = null
        var clickListener: RecyclerItemClickListener? = null
        var currentList = ArrayList<M>()

        @SuppressLint("NotifyDataSetChanged")
        fun setDataList(list: ArrayList<M>?) {
            if (list == null) {
                currentList.clear()
                notifyDataSetChanged()
                return
            }
            currentList = list
            notifyDataSetChanged()
        }

        open fun onViewHolderCreated(holder: BaseViewHolder<T>, type: Int) {}

        override fun getItemCount(): Int {
            return currentList.size
        }

        fun setOnItemClickListener(clickListener: RecyclerItemClickListener?) {
            this.clickListener = clickListener
        }

        var lastChecked = -1
        var currentChecked = -1

        var singleCheckable = false

        open fun onChecked(binding: T) {}

        open fun onCheckedChangeBinding(binding: T, animate: Boolean) {

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
            val vh: BaseViewHolder<T> = BaseViewHolder.create(parent, viewType, clickListener)
            if (singleCheckable) {
                vh.apply {
                    vh.itemView.setOnClickListener {
                        lastChecked = currentChecked
                        currentChecked = layoutPosition
                        if (lastChecked != currentChecked) {
                            onCheckedChangeBinding(vh.binding, true)
                            onChecked(vh.binding)
                        }
                    }
                }
            }
            onViewHolderCreated(vh, viewType)
            return vh
        }

        var autoSetData = true

        override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
            val model = currentList[position]
            bind(holder, position, model)
            holder.binding.apply {
                if (autoSetData) {
                    try {
                    //    setVariable(BR.data, model)
                    } catch (e: Exception) {
                        throw e
                    }
                }
                if (singleCheckable) {
                    onCheckedChangeBinding(this,false)
                }
                executePendingBindings()
            }
        }

        open fun bind(holder: BaseViewHolder<T>, position: Int, model: M) {}

        @LayoutRes
        open fun getItemLayoutId(position: Int, model: M): Int = layoutId

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            recyclerView.apply {
                mRecyclerView = this
                setHasFixedSize(true)
            }
        }

        fun getViewHolder(position: Int): BaseViewHolder<T>? {
            return mRecyclerView?.findViewHolderForLayoutPosition(position) as BaseViewHolder<T>?
        }

        fun getViewHolder(item: M): BaseViewHolder<T>? {
            try {
                val index = currentList.indexOf(item)
                return getViewHolder(index)
            } catch (e: java.lang.Exception) {

            }
            return null
        }

        override fun onFailedToRecycleView(holder: BaseViewHolder<T>): Boolean {
            return true
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            mRecyclerView = null
        }

        override fun getItemViewType(position: Int): Int {
            return getItemLayoutId(position, currentList[position])
        }
    }

    open class BaseViewHolder<T : ViewDataBinding>(
        val binding: T,
        clickListener: RecyclerItemClickListener?,
    ) : RecyclerView.ViewHolder(
        binding.root) {

        fun setFullSpan(fullSpan: Boolean) {
            itemView.layoutParams
                .apply {
                    if (this is StaggeredGridLayoutManager.LayoutParams) {
                        isFullSpan = fullSpan
                    }
                }
        }

        companion object {
            @JvmStatic
            fun <T : ViewDataBinding> create(
                parent: ViewGroup,
                @LayoutRes layoutId: Int,
                clickListener: RecyclerItemClickListener?,

                ): BaseViewHolder<T> {
                val binding: T = inflateBinding(parent, layoutId)

                return BaseViewHolder(binding, clickListener)
            }
        }

        var onClicked: Runnable? = null

        init {
            if (clickListener != null) {
                binding.root.setOnClickListener {
                    onClicked?.run()
                    clickListener.onClick(adapterPosition, itemViewType)
                }
            }
        }
    }
interface RecyclerItemClickListener {
    fun onClick(position: Int, viewType: Int)
}