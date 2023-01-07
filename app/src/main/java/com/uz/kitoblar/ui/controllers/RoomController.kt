package com.uz.kitoblar.ui.controllers

import androidx.room.*
import com.uz.kitoblar.applicationContext
import com.uz.kitoblar.currentTimeMillis

@Entity
class SearchBook: Model(){
    @ColumnInfo(name = "text")
    var text = ""

    @ColumnInfo(name = "bookId")
    var bookId = ""

    @ColumnInfo(name = "photo")
    var photo = ""
}

@Dao
interface SearchBookDao {
    @Query("SELECT * FROM searchbook")
    fun getAllSearches(): List<SearchBook>

    @Insert
    fun addBookSearch(book: SearchBook)

    @Delete
    fun deleteSearch(book: SearchBook)
}

@Database(entities = [SearchBook::class,MyBook::class], version = 3, exportSchema = true)
abstract class MyRoomDatabase : RoomDatabase() {
    abstract fun searchBookDao(): SearchBookDao
    abstract fun booksLibrary(): BooksLibrary
}

@Entity
class MyBook : Model(){
    var name = ""
    var about = ""
    var photo = ""
    var publisherId = ""
    var authorId = ""
    var authorName = ""
    var authorPhoto = ""
    var pdfUrl = ""
    var musicUrl = ""
    var part = 0
    var musicDurationDone = 0L
    var likes = 0
    var parentBook = ""
    var youTubeUrl = ""
    var viewsCount = 0
    var lastReadTime = currentTimeMillis()
    var duration = 0L
    var reading = 0
    var categoryId = ""
    var categoryName = ""
    var categoryPhoto = ""
    var rating = 0.0
    var userId = ""

    fun toBook(): Book {
        return Book().apply {
            this.id = this@MyBook.id
            this.name = this@MyBook.name
            this.about = this@MyBook.about
            this.photo = this@MyBook.photo
            this.publisherId = this@MyBook.publisherId
            this.author = Author().apply {
                this.id = this@MyBook.authorId
                authorName = this@MyBook.authorName
                this.photo = this@MyBook.authorPhoto
            }
            this.pdfUrl = this@MyBook.pdfUrl
            this.musicUrl = this@MyBook.musicUrl
            this.part = this@MyBook.part
            this.likes = this@MyBook.likes
            this.parentBook = this@MyBook.parentBook
            this.youTubeUrl = this@MyBook.youTubeUrl
            this.viewsCount = this@MyBook.viewsCount
            this.duration = this@MyBook.duration
            this.reading = this@MyBook.reading

            category = Category().apply {
                this.id = this@MyBook.categoryId
                this.name = this@MyBook.categoryName
                this.photo = this@MyBook.categoryPhoto
            }
            this.rating = this@MyBook.rating
        }
    }
}

@Dao
interface BooksLibrary {
    @Query("SELECT * FROM mybook")
    fun getBooks() : List<MyBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBooks(books: List<MyBook>)

    @Delete
    fun deleteBook(book: MyBook)

    @Delete
    fun deleteAll(list: List<MyBook>)

    @Query("SELECT * FROM mybook WHERE id=:mId")
    fun getBook(mId: String): MyBook

    @Query("DELETE FROM mybook WHERE id=:mId")
    fun deleteBookById(mId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBook(book: MyBook)

    @Update
    fun update(book: MyBook)
}

private var database: MyRoomDatabase? = null

fun roomDatabase(): MyRoomDatabase {
    return database ?: Room.databaseBuilder(
        applicationContext(),
        MyRoomDatabase::class.java,
        "roomDatabase"
    )
        .allowMainThreadQueries().build().also { database = it }
}