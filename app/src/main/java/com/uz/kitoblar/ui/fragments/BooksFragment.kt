package com.uz.kitoblar.ui.fragments

import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnPreDraw
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.*
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.*
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.inflateBinding
import com.uz.kitoblar.ui.myBooksLottie
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone

class BooksFragment : BaseFragment<BooksFragmentBinding>(R.layout.books_fragment) {

    private var feedList = arrayListOf<Model>()
    private lateinit var adapter: Adapter

    private var recyclerPool = RecyclerView.RecycledViewPool()

    private var loading = false
        set(value) {
            if (field == value) return
            field = value
            val list = getBooks()
            requireBinding().progressBar.visibleOrGone(list.isEmpty() && value)
            adapter.loading = value && list.isNotEmpty()
        }

    companion object {
        const val LAY_TYPE_BIG = R.layout.books_feed_layout_big
        const val LAY_TYPE_MEDIUM = R.layout.books_feed_layout_medium
        const val LAY_TYPE_TOP_READERS = R.layout.readers_layout

        enum class Type {
            TOP_READERS,
            POPULARS,
            CATEGORY,
            MUSIC,
            NEW
        }

        private var cache: ArrayList<BookItemMediumBinding>? = null

        fun animateRecyclers(recyclerView: RecyclerView) {
            recyclerView.doOnPreDraw {
                recyclerView.children.forEach {
                    it.apply {
                        alpha = 0f
                        scaleX = 0.9f
                        scaleY = 0.9f
                        animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                    }
                }
            }
        }

    }

    class Model(
        val type: Type,
        var categoryId: String,
        val title: String,
        val layType: Int = LAY_TYPE_MEDIUM,
        var books: ArrayList<Book>? = null,
        var loading: Boolean = false,
        var got: Boolean = false,
        var users: ArrayList<User>? = null
    ) : com.uz.kitoblar.ui.controllers.Model()

    override fun getActionBar(): View? {
        return inflateBinding<SearchLayoutBinding>(null, R.layout.search_layout).apply {
            root.setOnClickListener {
                openFragment(SearchFragment())
            }
        }.root
    }

    private fun loadFeedList() {
        //Load cats first
        booksController().apply {
            val cats = categories
            if (cats.isEmpty()) {
                toast("Internetga ulaning!")
                return
            }
            feedList.apply {
                //Firstly add populars
            //    add(Model(Type.TOP_READERS, "", "Top kitobxonlar", LAY_TYPE_TOP_READERS))
                add(Model(Type.POPULARS, "", "Trendlar", LAY_TYPE_BIG))
                //
                add(Model(Type.NEW, "", "Yangi kitoblar", LAY_TYPE_MEDIUM))
                //Add one from categories
                val cat = cats.first()
                add(Model(Type.CATEGORY, cat.id, cat.name, LAY_TYPE_MEDIUM))
                //Add music cats
                cats.forEach {
                    if (it.id != cat.id) {
                        add(Model(Type.CATEGORY, it.id, it.name, LAY_TYPE_MEDIUM))
                    }
                }
            }
        }
    }

    private fun getBooks() = feedList.filter {
        it.got && !it.books.isNullOrEmpty() || !it.users.isNullOrEmpty()
    }

    private fun updateList() {
        val gotBooks = getBooks()
        adapter.updateList(gotBooks as ArrayList<Model>)
    }

    fun loadNext(limit: Int = 2) {
        if (loading) {
            return
        }
        var allGot = true
        for (it in feedList) {
            if (!it.got) {
                allGot = false
                break
            }
        }
        if (allGot) {
            return
        }
        val listBookGot = arrayListOf<String>()
        for (it in feedList) {
            if (!it.got && !it.loading) {
                listBookGot.add(it.id)
            }
            if (listBookGot.size == limit) {
                break
            }
        }
        loading = true
        loadFeedBooks(listBookGot) {
            loading = false
            if (it) {
                loadNext()
            } else {
                updateList()
            }
        }
    }

    private fun loadFeedBooks(list: ArrayList<String>, result: (isEmpty: Boolean) -> Unit) {
        var gotBooksCount = 0
        var emptyCount = 0
        list.forEach { n ->
            feedList.find { it.id == n }?.apply {
                loading = true
                val filter = when (type) {
                    Type.POPULARS -> Filter.POPULARS
                    Type.MUSIC -> Filter.MUSIC
                    Type.NEW -> Filter.NEW
                    else -> Filter.POPULARS
                }
                booksController.loadBooks(
                    categoryId = categoryId,
                    filter = filter,
                    limit = 14,
                    result = object : Result<ArrayList<Book>>() {
                        override fun onResult(result: ArrayList<Book>) {
                            super.onResult(result)
                            gotBooksCount += 1
                            loading = false
                            got = true
                            if (result.isEmpty()) {
                                emptyCount += 1
                            }
                            books = ArrayList(result.sortedByDescending { it.viewsCount })
                            if (type == Type.NEW) {
                                books!!.reverse()
                            }
                            if (gotBooksCount == list.size) {
                                result(emptyCount == gotBooksCount)
                            }
                        }
                    }
                )
            }
        }
    }

    private var preload = 16

    private fun setPreloadItems() {
        cache = ArrayList()

        for (i in 0..preload) {
            val b = inflateBinding<BookItemMediumBinding>(
                requireBinding().recyclerView,
                R.layout.book_item_medium
            )
            cache!!.add(b)
        }

    }

    open class InnerAdapter(val hasInfo: Boolean = true) : BaseAdapter<Book, ViewDataBinding>(R.layout.book_item_big) {
        var type = LAY_TYPE_MEDIUM
        var id = ""
        var model: Model? = null

        override fun getItemViewType(position: Int): Int {
            return if (getItem(position).id == loadingLayId.toString()) super.getItemViewType(
                position
            ) else if (type == LAY_TYPE_MEDIUM) R.layout.book_item_medium else R.layout.book_item_big
        }

        override fun createBinding(parent: ViewGroup, viewType: Int): ViewDataBinding {
            if (viewType == R.layout.book_item_medium) {
                if (cache != null) {
                    val b = cache!!.getOrNull(0)
                    if (b != null) {
                        cache!!.removeAt(0)
                        if (cache!!.isEmpty()) {
                            cache = null
                        }
                        return b
                    }
                }
            }
            return super.createBinding(parent, viewType)
        }

        private var radius = dp(12f).toFloat()


        override fun bind(holder: BaseViewHolder<ViewDataBinding>, model: Book) {
            super.bind(holder, model)
            holder.apply {
                binding.apply {
                    if (clickListener == null) {
                        root.setOnClickListener {
                            this@InnerAdapter.model?.let {
                                clickListener?.invoke(holder)
                            }
                        }
                    }
                    when (this) {
                        is BookItemBigBinding -> {
                            photoView.apply {
                                cornerRadius = radius
                                setData(model.photo)
                            }
                            titleView.text = model.name
                            infoView.text = model.getInfo()
                        }
                        is BookItemMediumBinding -> {
                            photoView.apply {
                                cornerRadius = radius
                                setData(model.photo)
                            }
                            titleView.text = model.name
                            infoView.visibleOrGone(hasInfo)
                            infoView.text = model.getInfo()
                            playView.visibleOrGone(model.musicUrl.isNotEmpty())
                        }
                    }
                }
            }
        }
    }

    inner class Adapter : BaseAdapter<Model, ViewDataBinding>(R.layout.book_item_big) {
        override fun getItemViewType(position: Int): Int {
            try {
                return (getItem(position) as Model).layType
            } catch (e: Exception) {

            }
            return super.getItemViewType(position)
        }

        private fun setClickListenerM(holder: BaseViewHolder<ViewDataBinding>) {
            holder.apply {
                binding.apply {
                    when (this) {
                        is BooksFeedLayoutBigBinding -> {
                            recyclerView.adapter?.let {
                                (it as InnerAdapter).apply {
                                    clickListener = {
                                        val list = feedList.find { it.id == id }
                                        val book = list?.books?.get(it.layoutPosition)
                                        if (book != null) {
                                            openFragment(DetailsFragment(book.id))
                                        }
                                    }
                                }
                            }
                        }
                        is BooksFeedLayoutMediumBinding -> {
                            recyclerView.adapter?.let {
                                (it as InnerAdapter).apply {
                                    clickListener = {
                                        try {
                                            val list = feedList.find { it.id == id }
                                            val book = list?.books?.get(it.layoutPosition)
                                            if (book != null) {
                                                openFragment(DetailsFragment(book.id))
                                            }
                                        } catch (e: Exception) {
                                            //
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun holderCreated(
            holder: BaseViewHolder<ViewDataBinding>,
            itemType: Int
        ) {
            super.holderCreated(holder, itemType)
            holder.apply {
                binding.apply {
                    when (this) {
                        is BooksFeedLayoutBigBinding -> {
                            recyclerView.apply {
                                animateRecyclers(this)
                                itemAnimator = null
                                adapter = InnerAdapter()
                                setRecycledViewPool(recyclerPool)
                                layoutManager = LinearLayoutManager(
                                    context,
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )
                            }
                        }
                        is BooksFeedLayoutMediumBinding -> {
                            headerView.setOnClickListener {
                                val item = getItem(layoutPosition) as Model
                                openFragment(SearchResultsFragment(item.title.lowercase(),item.type == Type.NEW))
                            }
                            recyclerView.apply {
                                itemAnimator = null
                                animateRecyclers(this)
                                setRecycledViewPool(recyclerPool)
                                adapter = InnerAdapter()
                            }
                        }
                        is ReadersLayoutBinding -> {
                            recyclerView.apply {
                                adapter = object :
                                    BaseAdapter<User, UserItemBinding>(R.layout.user_item) {
                                    init {
                                        clickListener = {
                                            openFragment(UserProfileFragment((this.getItem(it.layoutPosition) as User)))
                                        }
                                    }
                                    override fun holderCreated(
                                        holder: BaseViewHolder<UserItemBinding>,
                                        itemType: Int
                                    ) {
                                        holder.binding.imageView.apply {
                                            circleCrop = true
                                        }
                                    }

                                    override fun bind(
                                        holder: BaseViewHolder<UserItemBinding>,
                                        model: User
                                    ) {
                                        holder.apply {
                                            binding.apply {
                                                imageView.setData(model.photo)
                                                nameView.text = model.name
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                setClickListenerM(this)
            }
        }

        override fun bind(holder: BaseViewHolder<ViewDataBinding>, model: Model, position: Int) {
            super.bind(holder, model, position)

            holder.apply {
                itemView.tag = model.title
                binding.apply {
                    when (this) {
                        is BooksFeedLayoutBigBinding -> {
                            titleView.text = model.title
                            val adapter = recyclerView.adapter as InnerAdapter
                            adapter.type = model.layType
                            adapter.id = model.id
                            adapter.model = model
                            adapter.updateList(model.books)
                        }
                        is BooksFeedLayoutMediumBinding -> {
                            titleView.text = model.title
                            val adapter = recyclerView.adapter as InnerAdapter
                            adapter.type = model.layType
                            adapter.id = model.id
                            adapter.model = model

                            adapter.updateList(model.books)
                        }
                        is ReadersLayoutBinding -> {
                            val adapter = recyclerView.adapter as BaseAdapter<User, UserItemBinding>
                            adapter.submitList(model.users as ArrayList<com.uz.kitoblar.ui.controllers.Model>?)
                        }
                    }
                }
                if (layoutPosition == currentList.lastIndex && model.id != loadingLayId.toString()) {
                    loadNext()
                }
            }
        }
    }

    private fun loadTopUsers() {
        booksController().loadTopUser {
            feedList.find { it.type == Type.TOP_READERS }?.apply {
                it.sortByDescending { it.readingBooks }
                users = it
                got = true
            }
            updateList()
            adapter.notifyItemChanged(0)
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
        val bookId = mainActivity().intent?.extras?.getString(BOOK_ID)
        mainActivity().intent?.removeExtra(BOOK_ID)
        if (bookId != null) {
            runOnUiThread(400) {
                openFragment(DetailsFragment(bookId))
            }
        }
    }

    override fun onViewCreated(binding: BooksFragmentBinding) {
        super.onViewCreated(binding)

        binding.apply {
            recyclerView.apply {
                adapter = Adapter().also { this@BooksFragment.adapter = it }
                setRecycledViewPool(recyclerPool)
                layoutManager = LinearLayoutManager(context)
            }
            progressBar.visibleOrGone(true)
            booksController.loadCategories(false) {
                if (it != null) {
                    loadFeedList()
               //     loadTopUsers()
                    loadNext(2)
                    setPreloadItems()
                } else {
                    loading = false
                    //Check internet
                }
            }

            if (currentUserId().isEmpty()) {
                preloadLottie(myBooksLottie)
            }
        }
    }
}