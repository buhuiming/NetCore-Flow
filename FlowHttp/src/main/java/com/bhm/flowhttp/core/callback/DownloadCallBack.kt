@file:Suppress("unused")

package com.bhm.flowhttp.core.callback



/** 事件执行的回调
 * Created by bhm on 2023/5/6.
 */
open class DownloadCallBack : ProgressCallBack<Any>() {

    private var _start: (() -> Unit)? = null

    private var _progress: ((progress: Int, bytesWritten: Long, contentLength: Long) -> Unit)? = null

    private var _success: ((response: Any) -> Unit)? = null

    private var _fail: ((e: Throwable?) -> Unit)? = null

    private var _complete: (() -> Unit)? = null

    fun start(value: () -> Unit) {
        _start = value
    }

    fun progress(value: (progress: Int, bytesWritten: Long, contentLength: Long) -> Unit) {
        _progress = value
    }

    fun success(value: (response: Any) -> Unit) {
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

    override fun onSuccess(response: Any) {
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