package com.uz.kitoblar.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.uz.kitoblar.applicationContext

fun getSharedPref(name: String, mode: Int = Context.MODE_PRIVATE): SharedPreferences =
    applicationContext().getSharedPreferences(name, mode)

fun clearSharedPref(name: String) = getSharedPref(name).edit { clear() }

fun SharedPreferences.clear() {
    edit { clear() }
}
@Deprecated("This class deprecated")
class SharedModel<T : Any>(sharedModelName: String, val model: T) {
    private var sharedPref = getSharedPref(sharedModelName)
    private var inited = false

    companion object {

        fun getFieldNamesAndValues(model: Any): HashMap<String, Any?> {
            val fields = model.javaClass.declaredFields
            val map = hashMapOf<String, Any?>()
            fields.forEach {
                if (it != null) {
                    val name = it.name
                    it.isAccessible = true
                    val value = it.get(model)
                    if (value != null)
                        map[name] = value
                }
            }
            return map
        }

        fun setFieldNamesAndValues(map: HashMap<String, Any?>, any: Any): Any {
            map.forEach {
                val field = any.javaClass.getDeclaredField(it.key)
                field.isAccessible = true
                if (field.type == it.value?.javaClass) {
                    field.set(any, it.value)
                }
            }
            return any
        }

        fun isPrimitive(clazz: Class<*>): Boolean {
            return clazz == String::class.java || clazz == Int::class.java || clazz == Boolean::class.java || clazz == Float::class.java
        }

        fun saveIntoSharedPref(model: Any, sharedPref: SharedPreferences) {
            val fields = getFieldNamesAndValues(model)
            saveIntoSharedPref(fields, sharedPref)
        }

        fun saveIntoSharedPref(
            hashMap: HashMap<String, Any?>,
            sharedPref: SharedPreferences
        ) {
            sharedPref.edit {
                hashMap.forEach {
                    val name = it.key
                    val value = it.value
                    if (value != null) {
                        when (value) {
                            is String -> {
                                putString(name, value)
                            }
                            is Boolean -> {
                                putBoolean(name, value)
                            }
                            is Float -> {
                                putFloat(name, value)
                            }
                            is Long -> {
                                putLong(name, value)
                            }
                            is Int -> {
                                putInt(name, value)
                            }
                        }
                    }

                }
            }
        }

    }

    fun saveModel(newModel: T) {
        val fields = getFieldNamesAndValues(newModel)
        setFieldNamesAndValues(fields, model)
        saveIntoSharedPref(fields, sharedPref)
    }

    fun clear() {
        sharedPref.clear()
        inited = false
    }

    private fun getFromSharedPref() {
        sharedPref.apply {
            val fields = model.javaClass.declaredFields
            val map = HashMap<String, Any?>()

            fields.forEach {
                if (it != null) {
                    val type = it.type
                    val name = it.name

                    val value =
                        when (type) {
                            String::class.java -> {
                                getString(name, "")
                            }
                            Int::class.java -> {
                                getInt(name, 0)
                            }
                            Float::class.java -> {
                                getFloat(name, 0f)
                            }
                            Boolean::class.java -> {
                                getBoolean(name, false)
                            }
                            Long::class.java -> {
                                getLong(name, 0L)
                            }
                            else -> {
                                null
                            }
                        }
                    map[name] = value
                }
            }
            setFieldNamesAndValues(map, model)
        }
        inited = true
    }

    init {
        getFromSharedPref()
    }
}
