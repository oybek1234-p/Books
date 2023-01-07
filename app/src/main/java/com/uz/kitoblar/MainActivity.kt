package com.uz.kitoblar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.uz.kitoblar.databinding.ActivityMainBinding
import com.uz.kitoblar.databinding.MusicDialogBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.PopupDialog
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetController
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.controllers.MusicController
import com.uz.kitoblar.ui.controllers.PermissionController
import com.uz.kitoblar.ui.fragments.BooksFragment
import com.uz.kitoblar.ui.fragments.DetailsFragment
import com.uz.kitoblar.ui.fragments.ProfileFragment
import com.uz.kitoblar.ui.fragments.ReadingFragment
import com.uz.kitoblar.ui.fragments.sheets.ForceShareSheet
import com.uz.kitoblar.ui.preloadLot
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.userNotLogged
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.DownloadController

lateinit var appInflater: LayoutInflater

class MainActivity : AppCompatActivity(), AccountInstance.AccountCallback {

    private fun currentFragmentId() = host.lastOrNull() ?: ""
    private fun lastFragmentId() = host.getOrNull(host.size - 2) ?: ""
    var currFocusEditView: View? = null
    private var currentPopup: PopupDialog? = null
    lateinit var sheetController: BottomSheetController
    private lateinit var sheetContainer: MaterialCardView

    fun removePopups() {
        currentPopup?.dismiss()
        currentPopup = null
    }

    fun showPopup(dialog: PopupDialog,view: View) {
        removePopups()
        currentPopup = dialog.apply { show(view) }
    }

    private var host = arrayListOf<String>()

    companion object {
        var navigating = false
    }

    fun showSnackBar(text: String) {
        Snackbar.make(container, text, Snackbar.LENGTH_SHORT).show()
    }

    //Critic error - must be reviewed - takes
    fun openFragment(fragment: Fragment, removeOld: Boolean, animate: Boolean = true) {
        val id = (fragment as BaseFragment<*>).classId
        val current = supportFragmentManager.findFragmentByTag(currentFragmentId())
        supportFragmentManager.commit {
            if (animate) {
                setCustomAnimations(
                    R.anim.fragment_open_anim,
                    R.anim.fragment_close_anim,
                    R.anim.fragment_pop_enter,
                    R.anim.fragment_pop_exit
                )
            }
            if (current != null) {
                if (removeOld) {
                    removeFragment(current)
                } else {
                    hide(current)
                }
            }
            if (supportFragmentManager.findFragmentByTag(id) != null) {
                show(fragment)
                fragment.onResume()
            } else {
                add(R.id.container, fragment, id)
            }
        }
        current?.onPause()
        host.remove(id)
        host.add(id)
        val s = host
        if (removeOld) {
            current?.apply {
                (this as BaseFragment<*>)
                s.remove(classId)
            }
        }
    }

    private fun FragmentTransaction.removeFragment(fragment: Fragment) {
        remove(fragment)
    }

    fun closePreviousFragment() {
        val s = host
        supportFragmentManager.apply {
            if (s.size > 1) {
                val previous = supportFragmentManager.findFragmentByTag(lastFragmentId())
                if (previous != null)
                    commit {
                        removeFragment(previous)
                        (previous as BaseFragment<*>)
                        s.remove(previous.classId)
                    }
            }
        }
    }

    fun closeLastFragment() {
        supportFragmentManager.apply {
            if (fragments.size == 1) return

            val current = findFragmentByTag(currentFragmentId())
            val previous = findFragmentByTag(lastFragmentId())

            commit {
                setCustomAnimations(
                    R.anim.fragment_pop_enter,
                    R.anim.fragment_pop_exit,
                    R.anim.fragment_pop_enter,
                    R.anim.fragment_pop_exit
                )

                if (current != null) {
                    removeFragment(current)
                }
                if (previous != null) {
                    show(previous)
                }
                if (current is BaseFragment<*>) {
                    this@MainActivity.host.remove(current.classId)
                }
            }
            previous?.onResume()
        }
    }

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var container: FrameLayout

    private var fragmentIds = hashMapOf<Int, String>()
    private var currentId = 0

    fun showBottomNav(show: Boolean) {
        bottomNav.apply {
            if (show == isVisible) {
                return
            }
            visibleOrGone(true)
            animate().alpha(if (show) 1f else 0f).translationY((if (show) 0 else height).toFloat())
                .setDuration(300).setInterpolator(
                    decelerateInterpolator
                ).withEndAction {
                    if (!show) {
                        visibleOrGone(false)
                    }
                }.start()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.size == 1) {
            finish()
        } else {
            closeLastFragment()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        toast("Permitted")
        PermissionController.getInstance().onPermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        PermissionController.getInstance().onActivityResult(requestCode, resultCode == RESULT_OK)
    }

    lateinit var musicPlayer: MusicController
    private lateinit var musicView: MusicDialogBinding

    override fun onDestroy() {
        DownloadController.pauseAllDownloads()
        super.onDestroy()
    }

    fun openHome() {
        bottomNav.selectedItemId = R.id.books
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        appInflater = layoutInflater
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        container = binding.container
        bottomNav = binding.bottomNavigationView
        musicView = binding.musicView
        sheetController = BottomSheetController()
        sheetContainer = findViewById(R.id.bottom_sheet_container)

        val dim = ColorDrawable()
        sheetController.apply {
            init(sheetContainer,container)
            slideOffsetData.observeForever {
                if (it > 0) {
                    if (it<0.6f) {
                        if (container.foreground == null) {
                            container.foreground = dim
                        }
                        dim.color = Color.argb(it, 0f, 0f, 0f)
                        dim.invalidateSelf()
                    }
                } else {
                    container.foreground = null
                }
            }
        }

        launchOnBack {
            DownloadController.create()
            musicPlayer = MusicController(musicView)
        }
        BooksController.getInstance()

        accountInstance().setCallback(this)

        bottomNav.apply {
            setOnItemSelectedListener {
                val itemId = it.itemId
                if (currentId != itemId) {
                    val id = fragmentIds[itemId]
                    currentId = itemId
                    val openFragment = if (id != null) {
                        supportFragmentManager.findFragmentByTag(id)
                    } else {
                        when (itemId) {
                            R.id.books -> BooksFragment()
                            R.id.reading -> ReadingFragment()
                            R.id.profile -> ProfileFragment()
                            else -> null
                        }
                    }
                    openFragment?.apply {
                        (this as BaseFragment<*>)
                        showBottomNav = true
                        backButtonEnb = false
                        fragmentIds[itemId] = classId
                        openFragment(this, removeOld = false, false)
                        return@setOnItemSelectedListener true
                    }
                }
                return@setOnItemSelectedListener false
            }
            selectedItemId = R.id.books
        }
        if (!userLogged()) {
            preloadLot(userNotLogged)
        }

    }

    override fun onSignIn() {
        showSnackBar("Akkauntga kirildi!")
    }

    override fun onSignOut() {
        showSnackBar("Akkaundtdan chiqildi")
        BooksController.getInstance().apply {
            myBooks.clear()
            myBooksGot = false
            likesLoaded = false
            myLikes.clear()
        }
    }

    private fun currentFragment() = supportFragmentManager.findFragmentByTag(currentFragmentId())

    override fun onResume() {
        super.onResume()
      //  currentFragment()?.onResume()
    }

    override fun onPause() {
        super.onPause()
      //  currentFragment()?.onPause()
    }
}

class Container @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {
    var activity: MainActivity = findActivity(context)!!

    private var gestureDetector = GestureDetector(context,object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            activity.sheetController.closeLastSheet()
            return super.onSingleTapUp(e)
        }
    })

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        var showing = false
        val controller = activity.sheetController
        controller.slideOffsetData.value?.let {
            showing = it > 0f
        }
        if (showing && !controller.animating) {
          //  gestureDetector.onTouchEvent(ev!!)
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

}
