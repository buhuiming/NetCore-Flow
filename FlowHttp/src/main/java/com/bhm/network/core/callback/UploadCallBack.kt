package com.bhm.network.core.callback


/** 事件执行的回调
 * Created by bhm on 2023/5/6.
 */
open class UploadCallBack<T> : ProgressCallBack<T>() {

    private var _start: (() -> Unit)? = null

    private var _progress: ((progress: Int, bytesWritten: Long, contentLength: Long) -> Unit)? = null

    private var _success: ((response: T) -> Unit)? = null

    private var _fail: ((e: Throwable?) -> Unit)? = null

    private var _complete: (() -> Unit)? = null

    override var code: Int = 0

    fun start(value: () -> Unit) {
        _start = value
    }

    fun progress(value: (progress: Int, bytesWritten: Long, contentLength: Long) -> Unit) {
        _progress = value
    }

    fun success(value: (response: T) -> Unit) {
        _success = value
    }

    fun fail(value: (e: Throwable?) -> Unit) {
        _fail = value
    }

    fun complete(value: () -> Unit) {
        _complete = value
    }

    override fun onStart(specifiedTimeoutMillis: Long) {
        super.onStart(specifiedTimeoutMillis)
        _start?.invoke()
    }

    override fun onProgress(progress: Int, bytesWritten: Long, contentLength: Long) {
        _progress?.invoke(progress, bytesWritten, contentLength)
    }

    override fun onSuccess(response: T) {
        _success?.invoke(response)
    }

    override fun onFail(e: Throwable?) {
        super.onFail(e)
        _fail?.invoke(e)
    }

    override fun onComplete() {
        super.onComplete()
        _complete?.invoke()
    }
}