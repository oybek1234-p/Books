package com.uz.kitoblar

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.rpc.context.AttributeContext.Auth
import com.uz.kitoblar.ui.controllers.*
import com.uz.kitoblar.ui.toast
import java.util.concurrent.TimeUnit

class User : Model() {
    var name = ""
    var photo = ""
    var number = ""
    var gmail = ""
    var seller = false
    var books = 0
    var likedBooks = 0
    var readingBooks = 0
    var level = 0

    fun toAuthor(): Author {
        return Author().apply {
            this.id = this@User.id
            this.name = this@User.name
            this.photo = this@User.photo
            this.books = this@User.books
        }
    }
}

fun getUserLevel(level: Int) = "$level level"
fun userLogged() = AccountInstance.getInstance().userLogged()
fun currentUserId() = AccountInstance.getInstance().currentUserId()
fun currentUser() = accountInstance().currentUser()

fun accountInstance() = AccountInstance.getInstance()
fun booksController() = BooksController.getInstance()

class AccountInstance {

    companion object {
        private var instance: AccountInstance? = null

        fun getInstance() = instance ?: AccountInstance().also { instance = it }
    }

    interface UserCallback {
        fun onChanged(user: User?, error: DatabaseError?)
    }

    interface AccountCallback {
        fun onSignIn()
        fun onSignOut()
    }

    private val userConfig = UserConfig()
    private val usersDatabase = databaseReference("users")
    private val interestsCollection = fireStoreCollection("interests")

    fun currentUserReference() = usersDatabase.child(currentUserId())

    fun currentUser() = userConfig.user
    fun currentUserId() = currentUser().id

    fun userLogged(): Boolean {
        return currentUserId().isNotEmpty()
    }

    var isUserLogging = false
    var userLoading = false

    private var userEventListener: ValueEventListener? = null

    private var eventListeners = arrayListOf<UserCallback>()

    private var accountCallback: AccountCallback? = null

    fun setCallback(accountCallback: AccountCallback) {
        this.accountCallback = accountCallback
    }

    fun removeUserCallback(callback: UserCallback) {
        eventListeners.remove(callback)
    }

    fun registerUserCallback(callback: UserCallback) {
        eventListeners.add(callback)
        callback.onChanged(currentUser(), null)
        if (userEventListener == null) {
            registerUserSnapshot()
        }
    }

    fun unregisterUserSnapshot() {
        userEventListener?.let {
            usersDatabase.removeEventListener(it)
        }
        eventListeners.clear()
    }

    private fun createUserWithEmailAndPassword(
        name: String,
        email: String,
        password: String,
        result: (isSuccess: Boolean, isNewUser: Boolean) -> Unit
    ) {
        if (userLogged()) {
            signOut()
        }
        isUserLogging = true
        firebaseAuth().createUserWithEmailAndPassword(email, password).addOnCompleteListener {
            if (it.exception != null) {
                throw it.exception!!
                handleError(it.exception)
            }
            onSignComplete()
            configureSignInUser(name, it.result, result)
        }
    }

    fun createUser(
        name: String,
        email: String,
        password: String,
        interests: ArrayList<Genre>,
        result: (isSuccess: Boolean) -> Unit
    ) {
        createUserWithEmailAndPassword(name, email, password) { suc, new ->
            if (suc) {
                interestsCollection.document(currentUserId()).set(mapOf(Pair("list", interests)))
            }
            result.invoke(suc)
        }
    }

    fun configurePhoneSignUser(
        name: String,
        interests: ArrayList<Genre>,
        result: (isSuccess: Boolean) -> Unit
    ) {
        if (userLogged()) {
            usersDatabase.child(currentUserId()).child("name").setValue(name)
            interestsCollection.document(currentUserId()).set(mapOf(Pair("list", interests)))
                .justResult(result)
        } else {
            toast("User not logged")
            result.invoke(false)
        }
    }

    fun signInWithEmailAndPassword(
        name: String,
        email: String,
        password: String,
        result: (isSuccess: Boolean, isNewUser: Boolean) -> Unit
    ) {
        if (userLogged()) {
            signOut()
        }
        isUserLogging = true
        try {
            firebaseAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener {
                onSignComplete()
                if (handleError(it.exception)) {
                    result.invoke(false, false)
                    return@addOnCompleteListener
                }
                handleError(it.exception)
                if (it.result != null) {
                    configureSignInUser(name, it.result, result)
                } else {
                    result.invoke(false, false)
                }
            }
        } catch (e: Exception) {

        }
    }

    private var phoneNumberOptions: PhoneAuthOptions? = null

    fun sendVerificationCodeWithPhoneNumber(
        activity: Activity,
        number: String,
        result: (verificationId: String?, error: Exception?, phoneAuthCr: PhoneAuthCredential?) -> Unit
    ) {
        if (number.isEmpty()) return
        isUserLogging = true
        phoneNumberOptions = PhoneAuthOptions.Builder(firebaseAuth())
            .setPhoneNumber(number)
            .setActivity(activity)
            .setTimeout(60L * 2, TimeUnit.SECONDS)
            .setCallbacks(object :
                PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    result(null, null, p0)
                }

                override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(p0, p1)
                    result.invoke(p0, null, null)
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    toast("Ko'p marta notugri terildi.Keginroq urinib koring! ")
                    result("", p0, null)
                    onSignComplete()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(phoneNumberOptions!!)
    }

    fun validatePhoneNumber(number: String): String {
        val stringBuilder = StringBuilder().apply {
            number.apply {
                val countryCode = "998"
                var code = ""
                var hasPlus = false
                var hasCode = false
                var numIndex = 0
                if (!number.startsWith('+')) {
                    code = number.substring(0, 2)
                } else {
                    hasPlus = true
                    code = number.substring(1, 3)
                }
                append("+")
                append(countryCode)
                if (code != countryCode) {
                    hasCode = false
                }
                if (hasPlus) {
                    numIndex += 1
                }
                if (hasCode) {
                    numIndex += 3
                }
                append(number.substring(numIndex, number.length))
            }
        }
        return stringBuilder.toString()
    }

    fun onSignComplete() {
        isUserLogging = false
    }

    private fun configureSignInUser(
        name: String,
        authResult: AuthResult,
        result: (isSuccess: Boolean, newUser: Boolean) -> Unit
    ) {
        val user = authResult.user
        val userInfo = authResult.additionalUserInfo
        if (user != null) {
            val newUser = User().apply {
                id = user.uid
                this.name =
                    name.ifEmpty { user.displayName ?: userInfo?.username ?: "" }
                photo = user.photoUrl?.toString() ?: ""
                gmail = user.email ?: ""
                number = user.phoneNumber ?: ""
            }
            userConfig.saveModel(newUser)
            eventListeners.forEach {
                it.onChanged(newUser, null)
            }
            var isNew = false
            if (userInfo?.isNewUser == true) {
                isNew = true
                usersDatabase.child(newUser.id).setValue(newUser)
            }
            accountCallback?.onSignIn()
            val success = configureUser()
            if (!success) {
                signOut()
            }
            registerUserSnapshot()
            result(success, isNew)
        } else {
            signOut()
        }
    }

    fun signInWithPhoneAuthCredential(
        name: String,
        authCredential: PhoneAuthCredential,
        result: (isSuccess: Boolean, isNewUser: Boolean) -> Unit
    ) {
        if (authCredential.smsCode == null) {
            result(false, false)
            onSignComplete()
            return
        }
        isUserLogging = true
        firebaseAuth().signInWithCredential(authCredential).addOnCompleteListener {
            onSignComplete()
            if (it.isSuccessful) {
                if (it.exception != null) {
                    toast("Kod notugri, qayta urinib koring!")
                    result.invoke(false, false)
                    handleError(it.exception)
                }
                if (it.result != null) {
                    configureSignInUser(name, it.result, result)
                }
            } else {
                result(false, false)
            }
        }
    }

    fun signOut() {
        userConfig.apply {
            clear()
            currentUser().id = ""
        }
        isUserLogging = false
        userLoading = false
        firebaseAuth().signOut()
        unregisterUserSnapshot()
        accountCallback?.onSignOut()
    }

    private fun configureUser(): Boolean {
        val signInFirebase = firebaseAuth().currentUser != null
        val appSignIn = userLogged()

        if ((!signInFirebase && appSignIn || signInFirebase && !appSignIn) && !userLoading) {
            //Sign out
            signOut()
            return false
        }
        return true
    }

    private fun registerUserSnapshot() {
        val userId = currentUserId()
        unregisterUserSnapshot()
        if (userId.isNotEmpty()) {
            userLoading = true
            userEventListener =
                usersDatabase.child(userId).addValueEventListener<User> { model, error ->
                    userLoading = false
                    if (error != null) {
                        handleError(error)
                    }
                    if (model == null && error == null) {
                        signOut()
                    }
                    if (model != null) {
                        userConfig.saveModel(model)
                        configureUser()
                    }
                    eventListeners.forEach {
                        it.onChanged(model, error)
                    }
                }
        }
    }

    fun isUserRegistered(gmail: String, result: (success: Boolean, registered: Boolean) -> Unit) {
        firebaseAuth().fetchSignInMethodsForEmail(gmail).addOnCompleteListener {
            if (handleError(it.exception)) {
                result(false, false)
            } else {
                result(it.isSuccessful, it.result?.signInMethods?.isEmpty() != true)
            }
        }
    }

    init {
        init()
    }

    private fun init() {
        isUserLogging = false

        //Load user from network if it is already logged
        if (userLogged()) {
            registerUserSnapshot()
        }
    }

    fun beSeller(result: (isSuccess: Boolean) -> Unit) {
        currentUserReference().child("seller").setValue(true).addOnCompleteListener {
            result.invoke(it.isSuccessful)
        }
    }
}