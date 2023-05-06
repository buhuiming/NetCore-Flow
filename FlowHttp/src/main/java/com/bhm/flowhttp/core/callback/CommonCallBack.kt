@file:Suppress("unused")

package com.bhm.flowhttp.core.callback



/** 事件执行的回调
 * Created by bhm on 2023/5/6.
 */
open class CommonCallBack<T> : SpecifiedTimeoutCallBack<T>() {

    private var _start: (() -> Unit)? = null

    private var _success: ((response: T) -> Unit)? = null

    private var _fail: ((e: Throwable?) -> Unit)? = null

    private var _complete: (() -> Unit)? = null

    fun start(value: () -> Unit) {
        _start = value
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

    override fun onSuccess(response: T) {
        _success?.invoke(response)
    }

    override fun onFail(e: Throwable?) {
        //可以在此处理异常，比如e is HttpException 401,404等问题
        super.onFail(e)
        _fail?.invoke(e)
    }

    override fun onComplete() {
        super.onComplete()
        _complete?.invoke()
    }
}