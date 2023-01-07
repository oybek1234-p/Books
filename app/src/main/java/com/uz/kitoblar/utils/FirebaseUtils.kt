package com.uz.kitoblar

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.uz.kitoblar.ui.toast
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty1

fun fireStoreInstance() = FirebaseFirestore.getInstance()
fun firebaseDatabase() = FirebaseDatabase.getInstance()
fun firebaseStorage() = FirebaseStorage.getInstance()
fun firebaseAuth() = FirebaseAuth.getInstance()

fun fireStoreCollection(name: String) = fireStoreInstance().collection(name)
fun databaseReference(name: String) = firebaseDatabase().getReference(name)
fun storageReference(name: String) = firebaseStorage().getReference(name)

fun hasFirebaseUser() = firebaseAuth().currentUser != null

val increaseField = FieldValue.increment(1)
val decreaseField = FieldValue.increment(-1)
val increaseFieldRealtime = ServerValue.increment(1)
val decreaseFieldRealtime = ServerValue.increment(-1)

fun getIncreaseValue(value: Long) = FieldValue.increment(value)

fun getIncreaseValueRealtime(value: Long) = ServerValue.increment(value)

fun increaseValue(increase: Boolean) = if (increase) increaseField else decreaseField

fun <R> DocumentReference.increase(field: KMutableProperty<R>,increase: Boolean) {
    update(field.name, increaseValue(increase))
}

fun <R> DatabaseReference.increase(field: KMutableProperty<R>,increase: Boolean) {
    setValue(if (increase) increaseFieldRealtime else decreaseFieldRealtime)
}

fun <T: Any> Task<T>.result(result: (isSuccess: Boolean, result:T) -> Unit) {
    addOnCompleteListener {
        result(it.isSuccessful,it.result)
    }
}

fun <T: Any> Task<T>.justResult(result: (isSuccess: Boolean) -> Unit) {
    addOnCompleteListener {
        result(it.isSuccessful)
        if (exception != null) {
            toast("${exception!!.message}")
        }
    }

}

inline fun <reified T> DatabaseReference.addValueEventListener(crossinline result: (model: T?, error: DatabaseError?) -> Unit): ValueEventListener {
    val listener = addValueEventListener(object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            result.invoke(null,error)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                result.invoke(null,null)
            } else {
                val model = snapshot.getValue(T::class.java)
                result.invoke(model,null)
            }
        }
    })
    return listener
}

fun <T> Query.get(
    response: (arrayList: ArrayList<T>, isSuccess: Boolean) -> Unit,
    clazz: Class<T>
) {
    get().addOnCompleteListener {
        it.apply {
            response(result.toObjectsType(clazz), isSuccessful)
        }
    }
}

fun <T> QuerySnapshot.toObjectsType(type: Class<T>): ArrayList<T> {
    val list: ArrayList<T> = try {
        toObjects(type) as ArrayList<T>
    } catch (e: Exception) {
        ArrayList()
    }
    return list
}

fun <R> CollectionReference.update(
    id: String,
    field: KMutableProperty<R>,
    value: Any,
    result: (isSuccess: Boolean) -> Unit = {}
) {
    if (id.isEmpty()) return
    document(id).update(field.name, value).justResult(result)
}

fun <R> DocumentReference.update(
    field: KMutableProperty<R>,
    value: Any,
    result: (isSuccess: Boolean) -> Unit = {}
) {
    update(field.name, value).justResult(result)
}

fun <T : Any> DocumentReference.increaseField(
    property: KMutableProperty<T>,
    increase: Boolean,
    result: (isSuccess: Boolean) -> Unit = {}
) {
    update(property.name, if (increase) increaseField else decreaseField).justResult(result)
}

fun CollectionReference.deleteDocument(docId: String) = document(docId).delete()



