package com.uz.kitoblar.ui.components.actionBar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.uz.kitoblar.*
import com.uz.kitoblar.ui.*
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.fragments.MobileNumberFragment

open class BaseFragment<T : ViewDataBinding>(var layId: Int) : Fragment() {
    private var binding: T? = null

    fun requireBinding() = binding!!
    private fun bindingView() = requireBinding().root

    val accountInstance = AccountInstance.getInstance()
    val booksController = BooksController.getInstance()

    fun isOffline() = false

    fun player() = mainActivity().musicPlayer

    fun preloadLottie(url: String) {
        requireContext().preloadLot(url)
    }

    fun requireLogin(invoke: () -> Unit) {
        if (userLogged()) {
            invoke.invoke()
        } else {
            openFragment(MobileNumberFragment())
        }
    }

    var backButtonEnb = true

    fun mainActivity() = requireActivity() as MainActivity

    val classId = randomId()

    fun openFragment(fragment: Fragment, removeLast: Boolean = false, animate: Boolean = true) {
        mainActivity().openFragment(fragment, removeLast, animate)
    }

    fun openSheet(sheet: BottomSheetBase<*>) {
        mainActivity().sheetController.showSheet(sheet)
    }

    fun checkForError(exception: Exception? = null) {
        //Check for exception
        //    toast("Internetga ulanish kutilmoqda")
    }

    fun closeLastFragment() {
        mainActivity().closeLastFragment()
    }

    fun showSnackBar(text: String) {
        if (activity != null) {
            mainActivity().showSnackBar(text)
        }
    }

    var showBottomNav = false

    fun closePreviousFragment() {
        mainActivity().closePreviousFragment()
    }

    private lateinit var containerView: LinearLayout
    private lateinit var statusBarBack: View

    private var mActionBar: ActionBar? = null

    fun requireActionBar() = mActionBar!!

    private var customActionBar: View? = null

    override fun onResume() {
        super.onResume()

        hideKeyboard(mainActivity())
        mainActivity().setLightStatusBar(lightStatusBar)
        mainActivity().removePopups()
        mainActivity().showBottomNav(showBottomNav)
        containerView.apply {
            setPadding(0, 0, 0, if (showBottomNav) dp(56) else 0)
        }
    }

    open fun getActionBar(): View? {
        //Create action bar or get custom one
        mActionBar = ActionBar().apply {
            title = "Title"
            this.backButtonEnabled = backButtonEnb
            backButton().setOnClickListener {
                closeLastFragment()
            }
            lightStatusBar = false
        }
        return mActionBar!!.requireBinding().root
    }

    private var statusBarBackColor = 0
    var lightStatusBar = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflateBinding(container, layId)

        containerView = LinearLayout(requireContext())

        containerView.apply {
            orientation = LinearLayout.VERTICAL

            getActionBar()?.apply {
                addView(this, layParams(MATCH_PARENT, WRAP_CONTENT))

                if (mActionBar == null) {
                    customActionBar = this
                }
                if (this is ViewGroup) {
                    setPadding(
                        paddingLeft,
                        statusBarHeight + paddingTop,
                        paddingRight,
                        paddingBottom
                    )
                }
            }

            addView(bindingView(), layParams(MATCH_PARENT, MATCH_PARENT))
        }

        onViewCreated(binding!!)

        return containerView
    }

    open fun onViewCreated(binding: T) {}
}