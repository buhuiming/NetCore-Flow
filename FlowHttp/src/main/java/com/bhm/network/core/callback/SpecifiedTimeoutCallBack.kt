package com.bhm.network.core.callback

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

/**
 * @author Buhuiming
 * @description: 指定超时，在规定的时间内没有结果(成功/失败)，则触发。用在提示用户网络环境不给力的情况
 * @date :2023/5/6
 */
abstract class SpecifiedTimeoutCallBack<T>: CallBackImp<T> {

    private var mainHandler: Handler? = null

    private var activityWeakReference: WeakReference<Handler.Callback?>

    private var _specifiedTimeout: (() -> Unit)? = null

    init {
        activityWeakReference = WeakReference<Handler.Callback?>(Handler.Callback { msg ->
            when (msg.what) {
                DELAY -> {
                    onSpecifiedTimeout()
                    done()
                }
            }
            false
        })
        mainHandler = Handler(Looper.getMainLooper()) { msg ->
            activityWeakReference.get()?.handleMessage(msg)
            false
        }
    }

    override fun onStart(specifiedTimeoutMillis: Long) {
        mainHandler?.sendEmptyMessageDelayed(DELAY, specifiedTimeoutMillis)
    }

    override fun onComplete() {
        done()
    }

    override fun onFail(e: Throwable?) {
        done()
    }

    @Synchronized
    private fun done() {
        mainHandler?.removeMessages(DELAY)
    }

    fun specifiedTimeout(value: () -> Unit) {
        _specifiedTimeout = value
    }

    override fun onSpecifiedTimeout() {
       _specifiedTimeout?.invoke()
    }

}

const val DELAY = 1