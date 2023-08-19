package com.example.ting.other

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.EditText
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.example.ting.init.AppInitializer
import com.example.ting.other.Constants.APP_SECRET
import com.example.ting.other.Constants.SHP_DATASTORE
import com.ximalaya.ting.android.opensdk.httputil.util.BASE64Encoder
import com.ximalaya.ting.android.opensdk.httputil.util.HMACSHA1
import com.ximalaya.ting.android.player.MD5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(SHP_DATASTORE)

fun Context.isConnectedNetwork() = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).let {
    it.getNetworkCapabilities(it.activeNetwork)?.hasCapability(NET_CAPABILITY_VALIDATED) ?: false
}

fun Long.convertNumber(): String = when {
    this < 10000 -> toString()
    this < 100000000 -> String.format("%.1f万", toDouble() / 10000)
    else -> DecimalFormat("0.#亿").format(toDouble() / 100000000)
}

fun String.sig(): String = MD5.md5(HMACSHA1.HmacSHA1Encrypt(BASE64Encoder.encode(this), APP_SECRET))

fun String.toast() = Toast.makeText(AppInitializer.mContext, this, Toast.LENGTH_SHORT).show()

class TextChangedListener {
    private var beforeTextChanged: ((CharSequence?, Int, Int, Int) -> Unit)? = null
    private var onTextChanged: ((CharSequence?, Int, Int, Int) -> Unit)? = null
    private var afterTextChanged: ((Editable?) -> Unit)? = null

    fun beforeTextChanged(block: (text: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
        beforeTextChanged = block
    }

    fun onTextChanged(block: (text: CharSequence?, start: Int, count: Int, after: Int) -> Unit) {
        onTextChanged = block
    }

    fun afterTextChanged(block: (text: Editable?) -> Unit) {
        afterTextChanged = block
    }

    fun build() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            beforeTextChanged?.invoke(s, start, count, after)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged?.invoke(s, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            afterTextChanged?.invoke(s)
        }
    }
}

inline fun EditText.addTextChangedListener(listener: TextChangedListener.() -> Unit): TextWatcher {
    val textChangedListener = TextChangedListener().apply(listener).build()
    addTextChangedListener(textChangedListener)
    return textChangedListener
}

fun <T> LifecycleOwner.collectWhenStarted(
    flow: Flow<T>,
    action: suspend (value: T) -> Unit
) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle).collect(action)
    }
}

fun <T> LifecycleOwner.collectLatestWhenStarted(
    flow: Flow<T>,
    action: suspend (value: T) -> Unit
) {
    lifecycleScope.launch {
        flow.flowWithLifecycle(lifecycle).collectLatest(action)
    }
}

fun LifecycleOwner.launchWithLifeCycle(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
): Job = lifecycleScope.launch(context, start) {
    repeatOnLifecycle(minActiveState, block)
}

context(LifecycleOwner)
fun <T> Flow<T>.collectWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (value: T) -> Unit
) {
    lifecycleScope.launch {
        flowWithLifecycle(lifecycle, minActiveState).collect(action)
    }
}

context(LifecycleOwner)
fun <T> Flow<T>.collectLatestWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (value: T) -> Unit
) {
    lifecycleScope.launch {
        flowWithLifecycle(lifecycle, minActiveState).collectLatest(action)
    }
}

inline fun RecyclerView.setOnItemClickListener(crossinline listener: (Int, RecyclerView.ViewHolder) -> Unit) {
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        val gestureDetector = GestureDetector(context, object : GestureDetector.OnGestureListener {
            override fun onDown(e: MotionEvent): Boolean = false

            override fun onShowPress(e: MotionEvent) {
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                findChildViewUnder(e.x, e.y)?.let { child ->
                    listener(getChildAdapterPosition(child), getChildViewHolder(child))
                }
                return false
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean =
                false

            override fun onLongPress(e: MotionEvent) {
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
        })

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            gestureDetector.onTouchEvent(e)
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        }
    })
}