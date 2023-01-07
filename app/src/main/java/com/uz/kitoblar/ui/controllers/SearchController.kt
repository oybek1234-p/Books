package com.uz.kitoblar.ui.controllers

import com.algolia.search.saas.*
import com.google.gson.GsonBuilder
import com.uz.kitoblar.handleError
import com.uz.kitoblar.ui.log
import org.json.JSONArray
import org.json.JSONObject

val gson = GsonBuilder().setLenient().create()!!

fun toGsonObject(t: Any): JSONObject? {
    try {
        return JSONObject(gson.toJson(t))
    } catch (e: Exception) {

    }
    return null
}

fun <T> toObjectArray(jsonObject: JSONArray, t: Class<T>): ArrayList<T> {
    val list = arrayListOf<T>()
    for (i in 0..jsonObject.length()) {
        try {
            val json = jsonObject.getJSONObject(i)
            val model = gson.fromJson(json.toString(), t)
            list.add(model)
        } catch (e: Exception) {

        }
    }
    return list
}

object AlgoliaSearch {
    private const val APP_ID = "5AWX8GKG3C"
    private const val API_KEY = "bc559f7f761f7c7157b97424ec851380"

    private lateinit var client: Client
    private lateinit var booksIndex: Index

    private var inited = false

    fun init() {
        if (inited) return
        client = Client(APP_ID, API_KEY)
        booksIndex = client.getIndex("books")
        inited = true
    }

    fun addBook(book: Book, result: (success: Boolean) -> Unit) {
        init()
        val bookObject = toGsonObject(book)
        if (bookObject != null) {
            booksIndex.addObjectAsync(bookObject, object : CompletionHandler {
                override fun requestCompleted(p0: JSONObject?, p1: AlgoliaException?) {
                    if (p1 != null) {
                        handleError(p1)
                        result.invoke(false)
                        return
                    }
                    if (p0 != null) {
                        result.invoke(true)
                    }
                }
            })
        } else {
            result.invoke(false)
        }
    }

    private var completionHandler: CompletionHandler? = null
    private var request: Request? = null

    fun cancelSearch() {
        request?.cancel()
        completionHandler = null
    }

    fun searchBooks(
        searchText: String,
        page: Int,
        limit: Int,
        isSearch: Boolean,
        result: (isSuccess: Boolean, result: ArrayList<Book>?, page: Int) -> Unit
    ) {
        init()
        val query = Query(searchText).apply {
            hitsPerPage = limit
            setPage(page)
            if (isSearch) {
                setAttributesToRetrieve("name", "photo")
            }
        }
        cancelSearch()
        completionHandler = object : CompletionHandler {
            override fun requestCompleted(p0: JSONObject?, p1: AlgoliaException?) {
                if (p1 != null) {
                    handleError(p1)
                    result.invoke(false, null, 0)
                    return
                }
                if (p0 != null) {
                    try {
                        val cPage = p0["nbPages"] as Int
                        val hits = p0["hits"] as JSONArray
                        val list = toObjectArray(hits, Book::class.java)
                        result.invoke(true, list, cPage)
                    } catch (e: Exception) {
                        result.invoke(true, null, 0)
                    }
                    log(p0.toString())
                } else {
                    result.invoke(false, null, 0)
                }
            }
        }
        request = booksIndex.searchAsync(query, completionHandler)
    }
}