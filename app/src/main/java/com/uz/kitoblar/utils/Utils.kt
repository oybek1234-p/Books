package com.uz.kitoblar

import android.R
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Vibrator
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.type.LatLng
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.PermissionController
import com.uz.kitoblar.ui.log
import com.uz.kitoblar.ui.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.ParseException
import java.util.*
import java.util.concurrent.TimeUnit


fun newId() = UUID.randomUUID().toString()
fun currentDate() = System.currentTimeMillis()

fun applicationContext() = MyApplication.applicationContext
fun handler() = MyApplication.handler
lateinit var displayMetrics: DisplayMetrics
var statusBarHeight = 0

val decelerateInterpolator = DecelerateInterpolator(2f)

fun initUtils() {
    displayMetrics = applicationContext().resources.displayMetrics
    statusBarHeight = loadStatusBarHeight()
}

@SuppressLint("InternalInsetResource")
fun loadStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int =
        applicationContext().resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = applicationContext().resources.getDimensionPixelSize(resourceId)
    }
    return result
}

fun dp(value: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        displayMetrics
    ).toInt()
}

@OptIn(DelicateCoroutinesApi::class)
fun launchOnBack(runnable: () -> Unit) {
    GlobalScope.launch(Dispatchers.IO) {
        runnable.invoke()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun launchOnMain(runnable: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) {
        runnable.invoke()
    }
}

fun findActivity(context: Context?): MainActivity? {
    if (context is Activity) return context as MainActivity
    if (context is ContextWrapper) return findActivity(context.baseContext)
    return null
}

fun Activity.setLightStatusBar(light: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = light
}

fun dp(value: Float): Int {
    return dp(value.toInt())
}

fun runOnUiThread(runnable: Runnable, delay: Int = 0) {
    handler().apply {
        if (delay != 0) {
            postDelayed(runnable, delay.toLong())
        } else {
            post(runnable)
        }
    }
}

fun runOnUiThread(delay: Int, unit: () -> Unit) {
    runOnUiThread(unit, delay)
}

fun cancelRunOnUiThread(runnable: Runnable?) {
    if (runnable == null) return
    handler().removeCallbacks(runnable)
}

fun vibrate(delay: Long) {
    val manager =
        applicationContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    manager.vibrate(delay, null)
}

fun openPdfInWeb(activity: Context, url: String) {
    if (url.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse("http://docs.google.com/viewer?url=$url"), "text/html")
        activity.startActivity(intent)
    } catch (e: Exception) {
        handleError(e)
    }
}

fun openPdfChooser(context: Context, url: String) {
    if (url.isEmpty()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        context.startActivity(intent)
    } catch (e: Exception) {
        openPdfInWeb(context, url)
        handleError(e)
    }
}

fun practice() {
    val imageView = ImageView(applicationContext())
}

fun getCurrentKeyboardLanguage() = (
        (applicationContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .currentInputMethodSubtype as InputMethodSubtype).languageTag

fun openMapIntent(context: Context, latLang: LatLng) {
    val latitude = latLang.latitude
    val longitude = latLang.longitude
    context.apply {
        val url =
            "yandexmaps://maps.yandex.ru/?pt=$latitude$longitude&z=12&l=map"
        val intentYandex = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intentYandex.setPackage("ru.yandex.yandexmaps")

        val uriGoogle = "geo:$latitude,$longitude"
        val intentGoogle = Intent(Intent.ACTION_VIEW, Uri.parse(uriGoogle))
        intentGoogle.setPackage("com.google.android.apps.maps")

        val title = "Tanlang"
        val chooserIntent = Intent.createChooser(intentGoogle, title)
        val arr = arrayOfNulls<Intent>(1)
        arr[0] = intentYandex
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arr)
        startActivity(chooserIntent)
    }
}

fun hideKeyboard(activity: Activity) {
    try {
        var view = activity.currentFocus
        if (view == null) {
            view = findActivity(activity)?.currFocusEditView
        }
        view?.apply {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).apply {
                hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    } catch (e: java.lang.Exception) {
        log(e)
    }
}


fun shakeView(
    view: View?,
    offset: Float = 24f,
    repeat: Int = 6,
    vibrate: Boolean = true,
) {
    if (view == null) return
    view.animate().translationX(offset).setListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(p0: Animator) {

        }

        override fun onAnimationEnd(p0: Animator) {
            if (repeat == 0) {
                view.animate().setListener(null)
                view.animate().translationX(0f).withEndAction {
                    view.translationX = 0f
                }.setInterpolator(DecelerateInterpolator(0.8f)).setDuration(80).start()
                return
            }

            shakeView(view, -(offset - 8f), repeat - 1, false)
        }

        override fun onAnimationCancel(p0: Animator) {

        }

        override fun onAnimationRepeat(p0: Animator) {

        }

    }).setInterpolator(DecelerateInterpolator(0.5f)).setDuration(60).start()
    if (vibrate) {
        vibrate(5)
    }
}

fun getDrawable(resId: Int) =
    AppCompatResources.getDrawable(applicationContext(), resId)

fun isViewInsideMotionEvent(motionEvent: MotionEvent, view: View): Boolean {
    if (motionEvent.x > view.left && motionEvent.x < view.right && motionEvent.y > view.top && motionEvent.y < view.bottom) {
        return true
    }
    return false
}

fun findView(viewGroup: ViewGroup, id: String): View? {
    val context = viewGroup.context
    val idRes: Int = context.resources.getIdentifier(id, "id", context.packageName)
    var view = viewGroup.findViewById<View>(idRes)
    if (view == null) {
        for (i in 0 until viewGroup.childCount) {
            val childView = viewGroup.getChildAt(i)
            if (childView is ViewGroup) {
                view = findView(childView, id)
                if (view != null) {
                    return view
                }
            }
        }
    }
    return view
}


fun addMediaToGallery(bitmap: Bitmap, name: String) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

        }

        applicationContext().contentResolver.apply {
            insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { it ->
                openOutputStream(it)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    output.close()
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
                update(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues, null, null)
            }
        }
    } catch (e: java.lang.Exception) {
        log(e)
    }
}

fun showKeyboard(view: View?) {
    val keyboardService =
        applicationContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    if (view != null) {
        view.requestFocus()
        findActivity(view.context)?.apply {
            currFocusEditView = view
        }
        keyboardService.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    } else {
        keyboardService.toggleSoftInput(
            InputMethodManager.SHOW_FORCED,
            InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    }
}

fun EditText.showKeyboard() {
    showKeyboard(this)
}

fun calculateBitmapColor(bitmap: Bitmap): Int {
    try {
        Bitmap.createScaledBitmap(bitmap, 1, 1, false).apply {
            val pixel = getPixel(0, 0)
            if (bitmap != this) {
                recycle()
            }
            return pixel
        }
    } catch (e: Exception) {
        log(e)
    }
    return 0
}

fun calcDrawableColor(drawable: Drawable): Int {
    try {
        val bitmap = drawable.toBitmap()
        return calculateBitmapColor(bitmap)
    } catch (e: java.lang.Exception) {
        log(e)
    }
    return 0
}

const val MILLISECOND = 0
const val SECOND = 1
const val MINUTE = 2
const val HOUR = 3
const val DAY = 4
const val WEEK = 5
const val MONTH = 6
const val YEAR = 7

const val DATE_NEW = 0
const val DATE_TODAY = 1
const val DATE_YESTERDAY = 2
const val DATE_THIS_WEEK = 3
const val DATE_THIS_MONTH = 4
const val DATE_THIS_YEAR = 5
const val DATE_NOW = 6

fun currentTimeMillis() = System.currentTimeMillis()

fun calculateDateWithType(date: Long): Pair<Int, Long> {
    var day = 0L
    var hh = 0L
    var mm = 0L
    try {
        val currentTime = currentTimeMillis()
        val timeDiff: Long = currentTime - date
        day = TimeUnit.MILLISECONDS.toDays(timeDiff)
        hh = (TimeUnit.MILLISECONDS.toHours(timeDiff) - TimeUnit.DAYS.toHours(day))
        mm =
            (TimeUnit.MILLISECONDS.toMinutes(timeDiff) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(
                    timeDiff
                )
            ))
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return if (mm <= 60 && hh != 0L) {
        if (hh <= 60 && day != 0L) {
            Pair(DAY, day)
        } else {
            Pair(HOUR, hh)
        }
    } else {
        Pair(MINUTE, mm)
    }
}

fun getString(id: Int): String {
    return applicationContext().getString(id)
}

fun getDateText(type: Int): String {
    return when (type) {
        DATE_NOW -> "Hozirgina"
        DATE_TODAY -> "Bugun"
        DATE_YESTERDAY -> "Kecha"
        DATE_THIS_WEEK -> "Bu hafta"
        DATE_THIS_MONTH -> "Bu oy"
        DATE_THIS_YEAR -> "Bu yil"
        DATE_NEW -> "Yangi"
        else -> "Ancha oldin"
    }
}

fun getDurationInMin(duration: Long) = (duration / 60000).toFloat().toString() + " min"

fun getDateDifferenceText(date: Long): String {
    val value = calculateDateWithType(date)
    val type = value.first
    val dateValue = value.second
    if (dateValue == 0L) {
        return getDateText(DATE_NOW)
    }
    return dateValue.toString() + " ${
        when (type) {
            HOUR -> "soat"
            DAY -> "kun"
            MONTH -> "oy"
            YEAR -> "yil"
            MINUTE -> "min"
            else -> "sekund"
        }
    } oldin"
}

fun calculateDateExtended(date: Long): Pair<Int, Long> {
    val cDate = calculateDateWithType(date)
    val dateV = cDate.second
    var type = DATE_NEW

    when (cDate.first) {
        DAY -> {
            type = when (dateV) {
                1L -> {
                    DATE_TODAY
                }
                2L -> {
                    DATE_YESTERDAY
                }
                in 3..29 -> {
                    DATE_THIS_MONTH
                }
                else -> {
                    DATE_THIS_YEAR
                }
            }
        }
        MINUTE -> {
            type = if (dateV <= 4) {
                DATE_NEW
            } else {
                DATE_TODAY
            }
        }
        HOUR -> {
            type = DATE_TODAY
        }
    }
    return Pair(type, dateV)
}

fun randomId() = UUID.randomUUID().toString()

class Point {
    var x = 0
    var y = 0
    var pointColor: Int = 0
    var nextPoint = Point()

    companion object {
        var points = arrayListOf<Point>()
    }
}

class PointColor(var color: Int, var first: com.uz.kitoblar.Point)

const val APP_TAG = "Kitoblar"

fun shareBook(view: View, book: Book) {
    PermissionController.getInstance().requestPermissions(
        view.context,
        0,
        arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ),
        object : PermissionController.PermissionResult {
            override fun onDenied() {

            }

            override fun onGranted() {
                Glide.with(view.context).asBitmap().load(book.photo)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val path = MediaStore.Images.Media.insertImage(
                                findActivity(view.context)?.contentResolver,
                                resource,
                                null,
                                null
                            )
                            if (path.isNullOrEmpty()) return
                            Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${book.name} $APP_TAG platformasi orqali o'qing"
                                )
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(path))
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                findActivity(view.context)?.startActivity(
                                    Intent.createChooser(
                                        this,
                                        "Kitobni ulashing..."
                                    )
                                )
                            }
                        }
                    })
            }
        })
}

fun installShortcutForBook(book: Book, result: () -> Unit) {
    val shortCutIntent = Intent(
        applicationContext(),
        MainActivity::class.java
    )

    shortCutIntent.action = Intent.ACTION_MAIN

    val addIntent = Intent()

    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortCutIntent)
    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Convertor")
    addIntent.putExtra(
        Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
        Intent.ShortcutIconResource.fromContext(
            applicationContext(),
            R.drawable.alert_dark_frame
        )
    )
    addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
    addIntent.putExtra("duplicate", false)
    applicationContext().sendBroadcast(addIntent)

}

const val BOOK_ID = "book_id"

fun createShortcutOnHome(
    @NonNull activity: Activity,
    book: Book
) {
    val shortcutIntent = Intent(activity, MainActivity::class.java)
    shortcutIntent.action = Intent.ACTION_MAIN
    shortcutIntent.putExtra(BOOK_ID, book.id)
    booksController().addBookToLibrary(book) {}
    Glide.with(activity).asBitmap().load(book.photo).into(object : SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val path = MediaStore.Images.Media.insertImage(
                activity.contentResolver,
                resource,
                book.name,
                null
            )

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                    val intent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, book.name)
                    intent.putExtra("duplicate", false)
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, path)
                    activity.sendBroadcast(intent)
                    println("added_to_homescreen")
                } else {
                    val shortcutManager = activity.getSystemService(
                        ShortcutManager::class.java
                    )!!
                    if (shortcutManager.isRequestPinShortcutSupported) {
                        val pinShortcutInfo = ShortcutInfo.Builder(activity, book.id)
                            .setIntent(shortcutIntent)
                            .setIcon(Icon.createWithBitmap(resource))
                            .setShortLabel(book.name)
                            .build()
                        shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                        println("added_to_homescreen")
                    } else {
                        println("failed_to_add")
                    }
                }
                activity.finishAffinity()
                activity.moveTaskToBack(true)
            } catch (e: Exception) {
                toast("Kitob bosh ekranda mavjud!")
            }

        }
    })

}

