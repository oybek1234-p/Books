package com.uz.kitoblar

import androidx.core.content.edit
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.utils.clear
import com.uz.kitoblar.utils.getSharedPref

class UserConfig {
    var user = User()
    var sharedPrefs = getSharedPref("userConfig")

    fun saveModel(newUser: User) {
        user = newUser
        sharedPrefs.edit {
            newUser.apply {
                putString(newUser::id.name,newUser.id)
                putInt(newUser::books.name,newUser.books)
                putString(newUser::gmail.name,newUser.gmail)
                putString(newUser::number.name,newUser.number)
                putString(newUser::photo.name,newUser.photo)
                putInt(newUser::likedBooks.name,newUser.likedBooks)
                putInt(newUser::level.name,newUser.level)
                putBoolean(newUser::seller.name,newUser.seller)
                putInt(newUser::readingBooks.name,newUser.readingBooks)
                putString(newUser::name.name,newUser.name)

            }
        }
    }

    private fun getUserFromCache() {
        sharedPrefs.apply {
            user.apply {
                id = getString(user::id.name,"").toString()
                books = getInt(user::books.name,0)
                gmail = getString(user::gmail.name,"").toString()
                number = getString(user::number.name,"").toString()
                photo = getString(user::photo.name,"").toString()
                likedBooks = getInt(user::likedBooks.name,0)
                level = getInt(user::level.name,0)
                seller = getBoolean(user::seller.name,false)
                readingBooks = getInt(user::readingBooks.name,0)
                name = getString(user::name.name,"").toString()
            }
        }
    }

    fun clear() {
        sharedPrefs.clear()
        user.id = ""
    }

    init {
        getUserFromCache()
    }
}