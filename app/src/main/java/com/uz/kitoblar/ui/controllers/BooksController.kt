package com.uz.kitoblar.ui.controllers

import android.os.Build
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Query
import com.uz.kitoblar.*
import com.uz.kitoblar.ui.toast

open class Result<T> {
    private var removed = false

    fun remove() {
        removed = true
    }

    fun result(result: T) {
        if (!removed) {
            onResult(result)
        }
    }

    open fun onResult(result: T) {}
}

enum class BookType {
    HARD_COVER,
    SOFT_COVER,
    MUSIC,
    PDF
}

fun getBookTypeText(type: BookType) = when(type) {
    BookType.PDF -> "Pdf"
    BookType.HARD_COVER -> "Qattiq muqova"
    BookType.MUSIC -> "Audio kitob"
    BookType.SOFT_COVER -> "Yumshoq muqova"
}

open class Model {
    @PrimaryKey
    var id = randomId()

    @ColumnInfo(name = "date")
    var date = currentDate()
}

class Cost : Model() {
    var bookId = "noId"
    var bookType = BookType.SOFT_COVER
    var price = 0L
}

class Book : Model() {
    var name = ""
    var about = ""
    var photo = ""
    var author = Author()
    var authorName = ""
    var pdfUrl = ""
    var musicUrl = ""
    var hashTag = "#book #popular"
    var category = Category()
    var reading = 0
    var pages = 0
    var duration = 1L
    var publisherId = ""
    var cost = Cost()
    var viewsCount = 0
    var likes = 0
    var parentBook = ""
    var part = 1
    var youTubeUrl = ""
    fun musicOrPdf() = musicUrl.isNotEmpty()

    var rating = 0.0
        get() {
            return if (field == 0.0) (3..5).random().toDouble() else field
        }

    fun getInfo(cat: Boolean = false) =
        "${category.name} $authorName"

    fun toMyBook() = MyBook().apply {
        this.id = this@Book.id
        this.name = this@Book.name
        this.about = this@Book.about
        this.photo = this@Book.photo
        this.publisherId = this@Book.publisherId
        this.authorId = this@Book.author.id
        this.authorName = this@Book.authorName
        this.authorPhoto = this@Book.author.photo
        this.pdfUrl = this@Book.pdfUrl
        this.musicUrl = this@Book.musicUrl
        this.part = this@Book.part
        this.likes = this@Book.likes
        this.parentBook = this@Book.parentBook
        this.youTubeUrl = this@Book.youTubeUrl
        this.viewsCount = this@Book.viewsCount
        this.duration = this@Book.duration
        this.reading = this@Book.reading
        this.categoryId = this@Book.category.id
        this.categoryName = this@Book.category.name
        this.categoryPhoto = this@Book.category.photo
        this.rating = this@Book.rating
    }
}

class Category() : Model() {
    var name = ""
    var photo = ""
}

const val userPlaceholder = "http://cdn.onlinewebfonts.com/svg/img_266351.png"

class Author : Model() {
    var name = "Author"
    var photo = ""
        get() {
            if (field.isEmpty()) return userPlaceholder
            return field
        }
    var books = 1
    var phone = "+998974801415"

    fun min(): Author {
        val author = Author()
        author.id = id
        author.name = name
        author.photo = photo
        author.books = books
        author.phone = phone
        return author
    }
}

fun <T : Model> ArrayList<T>.get(id: String) = find { it.id == id }
fun <T : Model> ArrayList<T>.remove(id: String) = remove(find { it.id == id })

enum class Filter {
    POPULARS,
    NEW,
    OLD,
    MUSIC
}

class Genre : Model {
    var name = ""
    var iconUrl = ""

    constructor(name: String, iconUrl: String) {
        this.name = name
        this.iconUrl = iconUrl
    }

    constructor()
}

class BooksController {

    private var accountInstance = AccountInstance.getInstance()
    var booksReference = fireStoreCollection("books")
    private var authorsReference = fireStoreCollection("authors")
    private var categoriesReference = fireStoreCollection("categories")
    var myBooks = arrayListOf<MyBook>()
    val myBooksReference = fireStoreCollection("myBooks")
    val usersReference = databaseReference("users")

    var books = arrayListOf<Book>()
    var authors = arrayListOf<Author>()
    var categories = arrayListOf<Category>()

    var relatedBooks = hashMapOf<String, ArrayList<Book>>()

    fun getBook(id: String) = books.find { it.id == id }

    init {
        initialize()
    }

    private fun canGetMyBooks() = userLogged() && currentUser().readingBooks > 0

    private fun initialize() {
        if (canGetMyBooks()) {
            getMyBooks()
        }
    }

    private fun getMyBooks() {
        val b = ArrayList(roomDatabase().booksLibrary().getBooks())
        books.addAll(b.map { it.toBook() })
        myBooks.addAll(b)
    }

    var genres = arrayListOf(
        Genre().apply {
            name = "\uD83E\uDD70  Qiziqarli"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDC7D Fantastika"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83E\uDD32 Islomiy"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDD75️ Detektiv"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDE02 Kulgili"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDC91 Drama"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDC68\uD83C\uDFFB\u200D\uD83D\uDE80 Ilm"
            iconUrl = ""
        },
        Genre().apply {
            name = "\uD83D\uDC68\u200D\uD83C\uDF93  Maktab darsliklari"
            iconUrl = ""
        }
    )

    companion object {
        private var INSTANCE: BooksController? = null
        fun getInstance() = INSTANCE ?: BooksController().also { INSTANCE = it }
    }

    /**
     * Categories
     */
    fun addNewCategory(category: Category, result: (success: Boolean) -> Unit) {
        categories.add(category)
        categoriesReference.document(category.id).set(category).justResult(result)
    }

    fun deleteCategory(id: String, result: (success: Boolean) -> Unit) {
        categories.removeIf { it.id == id }
        categoriesReference.document(id).delete().justResult(result)
        getMyBooks()
    }

    fun loadCategories(reload: Boolean, result: (list: ArrayList<Category>?) -> Unit) {
        val loadRun = {
            categoriesReference.get().addOnCompleteListener {
                val list = it.result.toObjectsType(Category::class.java)
                categories = list
                result(list)
            }
        }
        if (categories.isEmpty() || reload) {
            loadRun.invoke()
        } else {
            result.invoke(categories)
        }
    }

    fun addNewAuthor(author: Author, result: (isSuccess: Boolean) -> Unit) {
        authors.add(author)
        authorsReference.document(author.id).set(author).justResult(result)
    }

    fun deleteAuthor(id: String) {
        if (id.isEmpty()) return
        authors.remove(id)
        authorsReference.document(id).delete()
    }

    /**
     * Books
     */
    fun addNewBook(book: Book, result: (success: Boolean) -> Unit) {
        books.add(book)
        booksReference.document(book.id).set(book).justResult(result)
        val authorId = book.author.id
        authorsReference.document(authorId).increase(Author::books, true)
        authors.get(authorId)?.apply {
            books += 1
        }
    }

    fun deleteBook(bookId: String, done: () -> Unit) {
        val authorId = books.get(bookId)?.author?.id
        books.removeIf { it.id == bookId }
        booksReference.document(bookId).delete().justResult { done.invoke() }
        authorId?.let { authorsReference.document(it).increase(Author::books, false) }
    }

    fun loadBook(bookId: String, result: (b: Book?) -> Unit) {
        booksReference.document(bookId).get().addOnCompleteListener {
            if (it.isSuccessful) {
                val r = it.result.toObject(Book::class.java)
                result.invoke(r)
            } else {
                result.invoke(null)
            }
        }
    }

    fun loadBooks(
        forceCat: Boolean = false,
        authorId: String = "",
        categoryId: String = "",
        publisherId: String = "",
        startAfter: String = "",
        filter: Filter = Filter.NEW,
        limit: Int = 12,
        relatedBookId: String = "",
        result: Result<ArrayList<Book>>
    ) {
        var query = booksReference.limit(limit.toLong()).whereEqualTo("part", 1)
        if (filter == Filter.MUSIC) {
            query = query.whereNotEqualTo("musicUrl", "")
        } else {
            if (filter == Filter.POPULARS) {
                query = query.orderBy(Book::viewsCount.name, Query.Direction.DESCENDING)
            } else {
                query = query.orderBy(
                    "date",
                    when (filter) {
                        Filter.NEW -> {
                            Query.Direction.DESCENDING
                        }
                        Filter.OLD -> Query.Direction.ASCENDING
                        else -> Query.Direction.DESCENDING
                    }
                )
            }
        }
        if (startAfter.isNotEmpty()) {
            query = query.startAfter(startAfter)
        }
        if (authorId.isNotEmpty()) {
            query = query.whereEqualTo("author.id", authorId)
        }
        if (categoryId.isNotEmpty()) {
            query = query.whereEqualTo("category.id", categoryId)
        }
        if (publisherId.isNotEmpty()) {
            query = query.whereEqualTo(Book::publisherId.name, publisherId)
        }
        query.get().addOnCompleteListener {
            val list = it.result.toObjects(Book::class.java) as ArrayList
            list.remove(list.find { it.id == relatedBookId })
            if (list.isEmpty() && categoryId.isNotEmpty() && forceCat) {
                loadBooks(false, filter = Filter.POPULARS, result = result)
                return@addOnCompleteListener
            }
            val authorsList = list.map { it.author }
            authors.addAll(authorsList)
            if (relatedBookId.isNotEmpty()) {
                relatedBooks[relatedBookId] = list
            }
            books.addAll(list)
            configureBooks()
            result.result(list)
        }
    }

    //LIKE BOOK

    private var likesReference = fireStoreCollection("likes")
    var myLikes = hashMapOf<String, Like?>()

    fun getLikes(): ArrayList<Like> {
        val list = arrayListOf<Like>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            myLikes.forEach { t, u ->
                if (u != null) {
                    list.add(u)
                }
            }
        }
        return list
    }

    fun checkBookLiked(bookId: String, result: (liked: Boolean) -> Unit) {
        val id = getLikeBookId(bookId)
        val hasInCache = myLikes.contains(id)
        if (hasInCache) {
            val liked = myLikes[id] != null
            result(liked)
        } else {
            likesReference.document(id).get().addOnCompleteListener {
                var liked = false
                if (it.isSuccessful) {
                    if (it.result != null && it.result.exists()) {
                        liked = true
                    }
                }
                result(liked)
            }
        }
    }

    var likesLoaded = false

    fun loadLikedBooks(result: () -> Unit) {
        if (likesLoaded) return
        if (currentUserId().isEmpty()) return

        likesReference.whereEqualTo("userId", currentUserId()).get().addOnCompleteListener { it ->
            it.result.toObjects(Like::class.java).forEach {
                myLikes[getLikeBookId(it.bookId)] = it
            }
            likesLoaded = true
            result.invoke()
        }
    }

    class Like : Model() {
        var bookName = ""
        var bookPhoto = ""
        var userId = ""
        var bookId = ""
        var bookInfo = ""
    }

    private fun getLikeBookId(bookId: String) = currentUserId() + "&" + bookId

    fun likeBook(book: Book, like: Boolean, result: (success: Boolean) -> Unit) {
        val bookId = book.id
        checkBookLiked(bookId) {
            if (it == like) {
                result(true)
                return@checkBookLiked
            }
            val bId = getLikeBookId(bookId)
            if (like) {
                val l = Like().apply {
                    id = bId
                    bookName = book.name
                    this.bookId = book.id
                    bookPhoto = book.photo
                    bookInfo = book.about
                    userId = currentUserId()
                }
                likesReference.document(l.id).set(l).justResult(result)
                myLikes[bId] = l
                booksReference.document(bookId).update(Book::likes, increaseField)

            } else {
                myLikes[bId] = null
                booksReference.document(bookId).update(Book::likes, decreaseField)
                likesReference.document(bId).delete().justResult(result)
            }
        }
    }

    fun increaseBookView(bookId: String) {
        if (bookId.isEmpty()) return
        booksReference.document(bookId).increase(Book::viewsCount, true)
    }

    fun getAuthorBooks(authorId: String) = books.filter { it.author.id == authorId } as ArrayList
    fun getPopularBooks() = books.sortedByDescending { it.reading }
    fun getPublisherBooks(publisherId: String) = books.filter { it.publisherId == publisherId }
    fun getNewBooks() = books.sortedByDescending { it.date }
    fun getOldBooks() = books.sortedByDescending { it.date }

    fun getBooksForCategory(id: String) = books.filter { it.category.id == id }

    private fun configureBooks() {
        books = books.distinctBy { it.id } as ArrayList
        authors = authors.distinctBy { it.id } as ArrayList
    }

    private var myBooksLoading = false

    fun loadUserBooks(userId: String, result: Result<ArrayList<MyBook>>) {
        val query = myBooksReference.orderBy("date", Query.Direction.DESCENDING).whereEqualTo(
            "userId",
            userId
        )
        query.limit(20).get().addOnCompleteListener {
            val books = it.result.toObjectsType(MyBook::class.java)
            result.result(books)
        }
    }

    var myBooksGot = false

    fun loadMyBooks(r: () -> Unit) {
        myBooksLoading = true
        loadUserBooks(currentUserId(),
            object : Result<ArrayList<MyBook>>() {
                override fun onResult(result: ArrayList<MyBook>) {
                    myBooksLoading = false
                    myBooksGot = true
                    booksLibrary().apply {
                        deleteAll(myBooks.toList())
                        addBooks(result.toList())
                    }
                    myBooks = result
                    r.invoke()
                }
            })
    }

    private fun booksLibrary() = roomDatabase().booksLibrary()

    fun addBookToLibrary(book: Book, result: (success: Boolean) -> Unit) {
        if (currentUserId().isEmpty()) return
        val contains = myBooks.find { it.id == book.id } != null
        if (contains) {
            result.invoke(false)
            return
        }
        val myBook = book.toMyBook()
        myBook.userId = currentUserId()
        booksLibrary().addBook(myBook)
        myBooks.add(myBook)
        myBooksReference.document(book.id).set(myBook).justResult(result)
    }

    fun deleteBookFromLibrary(bookId: String, result: (s: Boolean) -> Unit) {
        booksLibrary().deleteBookById(bookId)
        myBooks.removeIf { it.id == bookId }
        myBooksReference.document(bookId).delete().justResult(result)
    }

    fun updateBook(book: MyBook, result: (isSuccess: Boolean) -> Unit) {
        booksLibrary().update(book)
        val index = myBooks.indexOfFirst { it.id == book.id }
        if (index == -1) {
            myBooks.add(book)
        } else {
            myBooks[index] = book
        }
        myBooksReference.document(book.id).set(book).justResult(result)
    }

    fun onSignOut() {
        myBooks.clear()
    }

    fun loadTopUser(list: (users: ArrayList<User>) -> Unit) {
        usersReference.orderByChild("readingBooks").limitToLast(12).get()
            .addOnCompleteListener { it ->
                if (it.result != null) {
                    val children = it.result.children
                    val myList = arrayListOf<User>()
                    children.forEach { a ->
                        val user = a.getValue(User::class.java)
                        if (user != null) {
                            myList.add(user)
                        }
                    }
                    toast("user ${myList.size}")
                    list.invoke(myList)
                } else {
                    toast("No users found")
                }
            }
    }

    fun getBookParts(bookId: String, notIncludeId: String = "") =
        books.filter { it.parentBook == bookId }

    fun getBooks() = books.filter { it.parentBook.isEmpty() }

    fun loadBooksParts(bookId: String, result: Result<ArrayList<Book>>) {
        booksReference.whereEqualTo("parentBook", bookId).get().addOnCompleteListener {
            val list = it.result.toObjects(Book::class.java) as ArrayList<Book>
            books.addAll(list)
            books = ArrayList(books.distinctBy { it.id })
            result.result(list)
        }
    }

    class Mundarija : Model() {
        var name = ""
        var bookId = ""
        var betOrMinute = 0
    }

    val mundarijalarReference = fireStoreCollection("mundarijalar")
    var mundarijalar = hashMapOf<String, ArrayList<Mundarija>>()

    fun loadMundarijalar(bookId: String, result: Result<ArrayList<Mundarija>>) {
        mundarijalarReference.whereEqualTo("bookId", bookId).get().addOnCompleteListener {
            val list = it.result.toObjects(Mundarija::class.java) as ArrayList<Mundarija>
            mundarijalar[bookId] = list
            result.result(list)
        }
    }

    fun addMundarija(bookId: String, mundarija: Mundarija, result: (success: Boolean) -> Unit) {
        mundarija.bookId = bookId
        mundarijalarReference.add(mundarija).justResult(result)
        mundarijalar.getOrPut(bookId) { arrayListOf<Mundarija>(mundarija) }
        getMyBooks()
    }

    fun deleteMundarija(mundarija: Mundarija, result: () -> Unit) {
        mundarijalar[mundarija.bookId]?.removeIf {
            it.id == mundarija.id
        }
        mundarijalarReference.deleteDocument(mundarija.id).justResult { result() }
    }

    private var commentsReference = fireStoreCollection("comments")

    class Comment : Model() {
        var userId = ""
        var comment = ""
        var userPhoto = ""
        var userName = ""
        var bookId = ""
        var rating = 0f
    }

    private var comments = hashMapOf<String, ArrayList<Comment>>()

    fun getCommentsForBook(id: String) = comments[id]

    fun addComment(comment: Comment, result: (s: Boolean) -> Unit) {
        comments.getOrPut(comment.bookId) { arrayListOf() }.add(comment)
        commentsReference.document(comment.id).set(comment).justResult(result)
    }

    fun deleteComment(comment: Comment) {
        comments.get(comment.bookId)?.removeIf { it.id == comment.id }
        commentsReference.document(comment.id).delete()
    }

    fun loadComments(
        bookId: String,
        size: Long = Long.MAX_VALUE,
        result: Result<ArrayList<Comment>>
    ) {
        commentsReference.whereEqualTo("bookId", bookId).limit(size).get().addOnCompleteListener {
            val list = it.result.toObjects(Comment::class.java) as ArrayList<Comment>
            comments[bookId] = list
            result.result(list)
        }
    }

    var costs = arrayListOf<Cost>()
    var costsReference = fireStoreCollection("bookCosts")

    fun addCosts(list: ArrayList<Cost>, done: () -> Unit) {
        costs.addAll(list)
        list.forEach { cost ->
            costsReference.document(cost.id).set(cost).addOnCompleteListener {
                if (cost == list.lastOrNull()) {
                    done.invoke()
                }
            }
        }
    }

    fun deleteCost(id: String, done: () -> Unit) {
        costs.removeIf { it.id == id }
        costsReference.deleteDocument(id).addOnCompleteListener {
            done()
        }
    }

    fun getCostsForBook(bookId: String, done: (list: ArrayList<Cost>) -> Unit) {
        if (bookId.isEmpty()) return
        costsReference.whereEqualTo("bookId", bookId).get().addOnCompleteListener {
            val result = it.result.toObjectsType(Cost::class.java)
            costs.addAll(result)
            done.invoke(result)
        }
    }

    fun getBookCost(bookId: String) = ArrayList(costs.filter { it.bookId == bookId })
}