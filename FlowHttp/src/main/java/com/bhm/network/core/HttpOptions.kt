@file:Suppress("unused")

package com.bhm.network.core

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bhm.network.base.HttpLoadingDialog
import com.bhm.network.core.HttpConfig.Companion.cancelable
import com.bhm.network.core.HttpConfig.Companion.httpLoadingDialog
import com.bhm.network.core.HttpConfig.Companion.writtenLength
import com.bhm.network.core.callback.CallBackImp
import com.bhm.network.define.*
import com.bhm.network.define.CommonUtil.logger
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import retrofit2.HttpException
import java.util.concurrent.TimeoutException

/**
 * Created by bhm on 2023/5/6.
 */
class HttpOptions(private val builder: Builder) {
    private var currentRequestDateTamp: Long = 0
    val activity: FragmentActivity
        get() = builder.activity
    var callBack: CallBackImp<*>? = null
        private set
    val isShowDialog: Boolean
        get() = builder.isShowDialog
    val isLogOutPut: Boolean
        get() = builder.isLogOutPut
    val dialog: HttpLoadingDialog?
        get() = builder.dialog
    private val isDefaultToast: Boolean
        get() = builder.isDefaultToast
    val readTimeOut: Int
        get() = builder.readTimeOut
    val connectTimeOut: Int
        get() = builder.connectTimeOut
    val okHttpClient: OkHttpClient?
        get() = builder.okHttpClient
    val filePath: String?
        get() = builder.filePath
    val fileName: String?
        get() = builder.fileName
    val isCancelable: Boolean
        get() = builder.isCancelable
    val isDialogDismissInterruptRequest: Boolean
        get() = builder.isDialogDismissInterruptRequest
    val isAppendWrite: Boolean
        get() = builder.isAppendWrite
    val jobKey: String
        get() = builder.jobKey

    fun writtenLength(): Long {
        return builder.writtenLength
    }

    val loadingTitle: String?
        get() = builder.loadingTitle
    val defaultHeader: HashMap<String, String>?
        get() = builder.defaultHeader
    private val delaysProcessLimitTimeMillis: Long
        get() = builder.delaysProcessLimitTimeMillis
    private val specifiedTimeoutMillis: Long
        get() = builder.specifiedTimeoutMillis
    val messageKey: String
        get() = builder.messageKey
    val codeKey: String
        get() = builder.codeKey
    val dataKey: String
        get() = builder.dataKey
    val successCode: Int
        get() = builder.successCode

    /*
    *  设置请求回调
    */
    fun <T: Any, E: Any> enqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
        this.callBack = callBack
        val job = CoroutineScope(Dispatchers.IO).launch {
            flow {
                emit(httpCall(api))
            }
                .catch {
                    logger(this@HttpOptions, "ThrowableConsumer-> ", it.message) //抛异常
                    if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                        delay(delaysProcessLimitTimeMillis)
                        doThrowableConsumer(callBack, it)
                    } else {
                        doThrowableConsumer(callBack, it)
                    }
                    JobManager.get().removeJob(builder.jobKey)
                }
                .onStart {
                    callBack?.onStart(specifiedTimeoutMillis)
                }
                .onCompletion {
                    callBack?.onComplete()
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (isActive) {
                        if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                            delay(delaysProcessLimitTimeMillis)
                            doBaseConsumer(callBack, it)
                        } else {
                            doBaseConsumer(callBack, it)
                        }
                    }
                    JobManager.get().removeJob(builder.jobKey)
                }
            currentRequestDateTamp = System.currentTimeMillis()
        }
        JobManager.get().addJob(builder.jobKey, job)
        return job
    }

    /*
    *  设置上传文件回调
    */
    fun <T: Any, E: Any> uploadEnqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
        return this.enqueue(api, httpCall, callBack)
    }

    /*
    *  设置文件下载回调
    */
    fun <T: Any, E: Any> downloadEnqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
        this.callBack = callBack
        val job = CoroutineScope(Dispatchers.IO).launch {
            flow {
                emit(httpCall(api))
            }
                .flowOn(Dispatchers.IO)
                .catch {
                    callBack?.onFail(it)
                    if (null != builder.dialog && builder.isShowDialog) {
                        builder.dialog?.dismissLoading(builder.activity)
                    }
                    JobManager.get().removeJob(builder.jobKey)
                }
                .onStart {
                    callBack?.onStart(specifiedTimeoutMillis)
                }
                .onCompletion {
                    callBack?.onComplete()
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                        delay(delaysProcessLimitTimeMillis)
                        doBaseConsumer(callBack, it)
                    } else {
                        doBaseConsumer(callBack, it)
                    }
                    JobManager.get().removeJob(builder.jobKey)
                }
            currentRequestDateTamp = System.currentTimeMillis()
        }
        JobManager.get().addJob(builder.jobKey, job)
        return job
    }

    private fun <E: Any> doBaseConsumer(callBack: CallBackImp<E>?, t: E) {
        if (builder.isDialogDismissInterruptRequest) {
            activity.lifecycleScope.launch(Dispatchers.Main) {
                if (isActive) {
                    success(callBack, t)
                }
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                success(callBack, t)
            }
        }
    }

    private fun <E: Any> success(callBack: CallBackImp<E>?, t: E) {
        callBack?.onSuccess(t)
        if (isShowDialog && null != dialog) {
            dialog?.dismissLoading(activity)
        }
    }

    private fun <T: Any> doThrowableConsumer(callBack: CallBackImp<T>?, e: Throwable) {
        if (builder.isDialogDismissInterruptRequest) {
            activity.lifecycleScope.launch(Dispatchers.Main) {
                if (isActive) {
                    fail(callBack, e)
                }
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                fail(callBack, e)
            }
        }
    }

    private fun <T: Any> fail(callBack: CallBackImp<T>?, e: Throwable) {
        callBack?.onFail(e)
        if (isShowDialog && null != dialog) {
            dialog?.dismissLoading(activity)
        }
        if (isDefaultToast) {
            if (e is HttpException) {
                if (e.code() == 404) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                } else if (e.code() == 504) {
                    Toast.makeText(activity, "请检查网络连接！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "请检查网络连接！", Toast.LENGTH_SHORT).show()
                }
            } else if (e is IndexOutOfBoundsException
                || e is NullPointerException
                || e is JsonSyntaxException
                || e is IllegalStateException
                || e is ResultException
            ) {
                Toast.makeText(activity, "数据异常，解析失败！", Toast.LENGTH_SHORT).show()
            } else if (e is TimeoutException) {
                Toast.makeText(activity, "连接超时，请重试！", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "请求失败，请稍后再试！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class Builder(val activity: FragmentActivity) {
        internal var isShowDialog = HttpConfig.isShowDialog
        internal var isCancelable = cancelable()
        internal var dialog = httpLoadingDialog
        internal var isDefaultToast = HttpConfig.isDefaultToast
        internal var readTimeOut = HttpConfig.readTimeOut
        internal var connectTimeOut = HttpConfig.connectTimeOut
        internal var okHttpClient = HttpConfig.okHttpClient
        internal var isLogOutPut = HttpConfig.isLogOutPut
        internal var filePath = HttpConfig.filePath
        internal var fileName = HttpConfig.fileName
        internal var writtenLength = writtenLength()
        internal var isAppendWrite = HttpConfig.isAppendWrite
        internal var loadingTitle = HttpConfig.loadingTitle
        internal var isDialogDismissInterruptRequest = HttpConfig.isDialogDismissInterruptRequest
        internal var defaultHeader = HttpConfig.defaultHeader
        internal var delaysProcessLimitTimeMillis = HttpConfig.delaysProcessLimitTimeMillis
        internal var specifiedTimeoutMillis = HttpConfig.specifiedTimeoutMillis
        internal var messageKey = HttpConfig.messageKey
        internal var codeKey = HttpConfig.codeKey
        internal var dataKey = HttpConfig.dataKey
        internal var successCode = HttpConfig.successCode
        internal var jobKey = System.currentTimeMillis().toString()

        init {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    if (isDialogDismissInterruptRequest) {
                        JobManager.get().removeJob(jobKey)
                    }
                    if (activity.isTaskRoot) {
                        JobManager.get().clear()
                    }
                    dialog?.close()
                    dialog = null
                    activity.lifecycle.removeObserver(this)
                }
            })
        }

        /**
         * 设置请求loading页面
         */
        fun setLoadingDialog(dialog: HttpLoadingDialog?) = apply {
            this.dialog = dialog
        }

        /**
         * 设置请求loading页面的属性
         * @param isShowDialog 是否显示loading页面 true：显示，false：不显示。默认为false
         * @param cancelable 按返回键是否关闭loading页面，true：关闭，false：不关闭(拦截)。默认为false
         * @param dialogDismissInterruptRequest loading页面关闭后，是否终止请求 true：终止请求，false：不终止请求。默认为true
         */
        fun setDialogAttribute(
            isShowDialog: Boolean,
            cancelable: Boolean,
            dialogDismissInterruptRequest: Boolean
        ) = apply {
            this.isShowDialog = isShowDialog
            isCancelable = cancelable
            this.isDialogDismissInterruptRequest = dialogDismissInterruptRequest
        }

        /**
         * 是否使用Toast提示，默认为false
         */
        fun setIsDefaultToast(isDefaultToast: Boolean) = apply {
            this.isDefaultToast = isDefaultToast
        }

        /**
         * 设置请求超时 单位秒
         * @param readTimeOut 读取超时
         * @param connectTimeOut 连接超时
         */
        fun setHttpTimeOut(readTimeOut: Int, connectTimeOut: Int) = apply {
            this.readTimeOut = readTimeOut
            this.connectTimeOut = connectTimeOut
        }

        /** 不推荐使用，使用此方法，将取消默认的设置，包括但不限于日志，缓存，下载，上传，网络，SSL。
         * @param okHttpClient
         */
        @Deprecated("使用此方法，将取消默认的设置，包括但不限于日志，缓存，下载，上传，网络，SSL。",
            ReplaceWith("apply { this.okHttpClient = okHttpClient }")
        )
        fun setOkHttpClient(okHttpClient: OkHttpClient?) = apply {
            this.okHttpClient = okHttpClient
        }

        /**
         * 是否输出日志，默认false不输出
         */
        fun setIsLogOutPut(isLogOutPut: Boolean) = apply {
            this.isLogOutPut = isLogOutPut
        }

        /**
         * 设置请求loading页面提示语
         */
        fun setLoadingTitle(loadingTitle: String?) = apply {
            this.loadingTitle = loadingTitle
        }

        /**
         * 设置请求默认的header
         */
        fun setDefaultHeader(defaultHeader: HashMap<String, String>?) = apply {
            this.defaultHeader = defaultHeader
        }

        /**
         * 设置下载文件的属性
         * @param mFilePath 文件路径，以\结尾
         * @param mFileName 文件名称
         * @param mAppendWrite 是否追加写入 true为追加写入
         * @param mWrittenLength 原文件的长度
         */
        fun setDownLoadFileAtr(
            mFilePath: String?,
            mFileName: String?,
            mAppendWrite: Boolean,
            mWrittenLength: Long
        ) = apply {
            filePath = mFilePath
            fileName = mFileName
            this.isAppendWrite = mAppendWrite
            writtenLength = mWrittenLength
        }

        /**
         * 设置请求成功/失败之后，再过[delaysProcessLimitTimeMillis]秒后去处理结果
         */
        fun setDelaysProcessLimitTimeMillis(delaysProcessLimitTimeMillis: Long) = apply {
            this.delaysProcessLimitTimeMillis = delaysProcessLimitTimeMillis
        }

        /**
         * 指定超时，在规定的时间内[specifiedTimeoutMillis]没有返回结果(成功/失败)，比如提示用户网络环境不给力的情况
         */
        fun setSpecifiedTimeoutMillis(specifiedTimeoutMillis: Long) = apply {
            this.specifiedTimeoutMillis = specifiedTimeoutMillis
        }

        /**
         * 设置请求json的解析结构体
         * @param messageKey 提示语字段名
         * @param codeKey 返回码字段名
         * @param dataKey 数据字段名
         * @param successCode 成功时的状态码值，默认[OK_CODE]
         */
        fun setJsonCovertKey(messageKey: String = MESSAGE_KEY,
                             codeKey: String = CODE_KEY,
                             dataKey: String = DATA_KEY,
                             successCode: Int = OK_CODE) = apply {
            this.messageKey = messageKey
            this.codeKey = codeKey
            this.dataKey = dataKey
            this.successCode = successCode
        }

        fun build(): HttpOptions {
            return HttpOptions(this)
        }
    }

    companion object {
        @JvmStatic
        fun create(activity: FragmentActivity) = Builder(activity)

        @JvmStatic
        fun getDefaultHttpOptions(activity: FragmentActivity): HttpOptions {
            return create(activity)
                .setLoadingDialog(HttpLoadingDialog.defaultDialog)
                .build()
        }
    }
}