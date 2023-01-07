package com.uz.kitoblar.ui.fragments

import android.view.Gravity
import android.view.View
import androidx.core.view.children
import androidx.lifecycle.ReportFragment
import com.google.firebase.database.DatabaseError
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.EmtyViewBinding
import com.uz.kitoblar.databinding.ProfileFragmentBinding
import com.uz.kitoblar.ui.*
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.showPopup
import com.uz.kitoblar.ui.controllers.Author
import com.uz.kitoblar.ui.controllers.userPlaceholder

class ProfileFragment : BaseFragment<ProfileFragmentBinding>(R.layout.profile_fragment) {

    private var currentUser: User? = null
    private var emptyView: EmtyViewBinding? = null

    fun updateUser() {
        requireBinding().apply {
            currentUser?.apply {
                photoView.apply {
                    setData(photo.ifEmpty { userPlaceholder })
                }
                nameView.text = name
                textView9.text = getUserLevel(level)
                phoneView.text = number
            }
        }
    }

    private var callback = object : AccountInstance.UserCallback {
        override fun onChanged(user: User?, error: DatabaseError?) {
            currentUser = user
            if (lastWasSeller != currentUser?.seller) {
                sellerChanges = true
                updateScreenCallback.invoke()
            } else {
                updateUser()
            }
        }
    }

    private val updateScreenCallback = {
        onViewCreated(requireBinding())
    }

    private var lastWasSeller = false
    private var sellerChanges = false

    override fun onResume() {
        super.onResume()

        accountInstance.registerUserCallback(callback)
        if (userLogged() && emptyView != null || !userLogged() && emptyView == null) {
            updateScreenCallback.invoke()
        }
        if (lastWasSeller != currentUser().seller) {
            sellerChanges = true
            updateScreenCallback.invoke()
        }
    }

    override fun onPause() {
        super.onPause()
        accountInstance.removeUserCallback(callback)
    }

    override fun getActionBar(): View? {
        return null
    }

    var backEnabled = false

    var photoLoading = false
        set(value) {
            field = value
            requireBinding().progressBar.visibleOrGone(value, 1)
        }

    override fun onViewCreated(binding: ProfileFragmentBinding) {
        super.onViewCreated(binding)

        preloadLottie(login)

        binding.apply {
            if (!userLogged()) {
                emptyView = inflateBinding<EmtyViewBinding>(null, R.layout.emty_view).apply {
                    setData(
                        userNotLogged,
                        "Akkauntga kiring",
                        "Koproq funksiyalarga ega buling,va havsiz his qiling",
                        "Kirish"
                    ) {
                        openFragment(MobileNumberFragment(), false)
                    }
                    topView.visibleOrGone(false)
                    for (i in 0 until container.childCount) {
                        container.children.find { it.id != topView.id }?.let {
                            container.removeView(it)
                        }
                    }
                    container.addView(root, container.layParams(MATCH_PARENT, MATCH_PARENT).apply {
                        gravity = Gravity.CENTER
                    })

                }
            } else {
                actionView.setOnClickListener {
                    showPopup(it) {
                        addItem(0, "Chiqish", R.drawable.ic_logout) {
                            accountInstance.signOut()
                            updateScreenCallback.invoke()
                        }
                    }
                }
                backButton.visibleOrGone(backEnabled)
                topView.visibleOrGone(true)
                photoView.circleCrop = true
                emptyView?.apply {
                    container.removeView(root)
                    emptyView = null
                }
                updateUser()
                editInfo.setOnClickListener {
                    //Open edit user info
                    openFragment(EditUserInfoFragment(), false)
                }
                editPhoto.setOnClickListener {
                    if (photoLoading) return@setOnClickListener
                    //Open choose galleryop
                    openFragment(
                        GalleryFragment({
                            val path = it.first()
                            currentUser?.photo = path
                            updateUser()
                            photoLoading = true
                            uploadImage(requireContext(), path) {
                                photoLoading = false
                                it?.let {
                                    accountInstance.currentUserReference().child("photo")
                                        .setValue(it)
                                }
                            }
                        }, 1),
                        false
                    )
                }
                container.apply {
                    topView.setPadding(0, statusBarHeight,0,paddingBottom)
                    if (sellerChanges) {
                        for (i in 0 until container.childCount) {
                            container.children.find { it.id != topView.id }?.let {
                                container.removeView(it)
                            }
                        }
                        sellerChanges = false
                    }
                    lastWasSeller = currentUser().seller
                    addBackgroundText("")
//                    addInfoButton(R.drawable.ic_settings, "Sozlamalar") {
//
//                    }
                    if (!currentUser().seller) {
                        addInfoButton(R.drawable.ic_add_library, "Sotuvchi bo'lish") {
                            openFragment(BeSellerFragment(), false)
                        }
                    } else {
                        addInfoButton(R.drawable.ic_add_box, "Kitob qo'shish") {
                            openFragment(AddBookFragment())
                        }
                        addInfoButton(R.drawable.book_icon, "Mening kitoblarim") {
                            openFragment(AuthorBooksFragment(Author().apply {
                                id = currentUserId()
                                name = currentUser().name
                                photo = currentUser().photo
                                books = currentUser().books
                            }))
                        }
                    }
//                    addInfoButton(R.drawable.ic_help, "Savol berish") {
//                        openFragment(ReportFragment())
//                    }
                }
            }
        }
    }
}