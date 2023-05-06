@file:Suppress("unused")

package com.bhm.flowhttp.core

import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bhm.flowhttp.base.HttpLoadingDialog
import com.bhm.flowhttp.core.HttpConfig.Companion.cancelable
import com.bhm.flowhttp.core.HttpConfig.Companion.httpLoadingDialog
import com.bhm.flowhttp.core.HttpConfig.Companion.writtenLength
import com.bhm.flowhttp.core.callback.CallBackImp
import com.bhm.flowhttp.define.*
import com.bhm.flowhttp.define.CommonUtil.logger
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.HttpException
import java.util.concurrent.TimeoutException

/**
 * Created by bhm on 2023/5/6.
 */
class HttpBuilder(private val builder: Builder) {
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
    val jobManager: JobManager
        get() = builder.jobManager
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
        val job = activity.lifecycleScope.launch {
            flow<E> {
                val entity = httpCall(api)
                if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                    delay(delaysProcessLimitTimeMillis)
                    doBaseConsumer(callBack, entity)
                } else {
                    doBaseConsumer(callBack, entity)
                }
            }
                .catch {
                    logger(this@HttpBuilder, "ThrowableConsumer-> ", it.message) //抛异常
                    if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                        delay(delaysProcessLimitTimeMillis)
                        doThrowableConsumer(callBack, it)
                    } else {
                        doThrowableConsumer(callBack, it)
                    }
                }
                .flowOn(Dispatchers.IO)
                .onStart {
                    callBack?.onStart(specifiedTimeoutMillis)
                }
                .onCompletion {
                    callBack?.onComplete()
                }
                .flowOn(Dispatchers.Main)
                .collect()
            currentRequestDateTamp = System.currentTimeMillis()
        }
        builder.jobManager.add(job)
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
        val job = activity.lifecycleScope.launch {
            flow<E> {
                val entity = httpCall(api)
                if (System.currentTimeMillis() - currentRequestDateTamp <= delaysProcessLimitTimeMillis) {
                    delay(delaysProcessLimitTimeMillis)
                    doBaseConsumer(callBack, entity)
                } else {
                    doBaseConsumer(callBack, entity)
                }
            }
                .flowOn(Dispatchers.IO)
                .catch {
                    callBack?.onFail(it)
                    if (null != builder.dialog && builder.isShowDialog) {
                        builder.dialog?.dismissLoading(builder.activity)
                    }
                    builder.jobManager.removeJob()
                }
                .onStart {
                    callBack?.onStart(specifiedTimeoutMillis)
                }
                .onCompletion {
                    callBack?.onComplete()
                }
                .flowOn(Dispatchers.Main)
                .collect()
            currentRequestDateTamp = System.currentTimeMillis()
        }
        builder.jobManager.add(job)
        return job
    }

    private fun <E: Any> doBaseConsumer(callBack: CallBackImp<E>?, t: E) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            callBack?.onSuccess(t)
            if (isShowDialog && null != dialog) {
                dialog?.dismissLoading(activity)
            }
        }
    }

    private fun <T: Any> doThrowableConsumer(callBack: CallBackImp<T>?, e: Throwable) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
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
    }

    class Builder(val activity: FragmentActivity) {
        internal var jobManager: JobManager = JobManager()
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

        init {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    jobManager.clear()
                    activity.lifecycle.removeObserver(this)
                }
            })
        }

        fun setLoadingDialog(dialog: HttpLoadingDialog?) = apply {
            this.dialog = dialog
        }

        fun setDialogAttribute(
            isShowDialog: Boolean,
            cancelable: Boolean,
            dialogDismissInterruptRequest: Boolean
        ) = apply {
            this.isShowDialog = isShowDialog
            isCancelable = cancelable
            this.isDialogDismissInterruptRequest = dialogDismissInterruptRequest
        }

        fun setIsDefaultToast(isDefaultToast: Boolean) = apply {
            this.isDefaultToast = isDefaultToast
        }

        fun setHttpTimeOut(readTimeOut: Int, connectTimeOut: Int) = apply {
            this.readTimeOut = readTimeOut
            this.connectTimeOut = connectTimeOut
        }

        /** 不推荐使用，使用此方法，将取消默认的设置，包括但不限于日志，缓存，下载，上传，网络，SSL。
         * @param okHttpClient
         * @return
         */
        @Deprecated("使用此方法，将取消默认的设置，包括但不限于日志，缓存，下载，上传，网络，SSL。",
            ReplaceWith("apply { this.okHttpClient = okHttpClient }")
        )
        fun setOkHttpClient(okHttpClient: OkHttpClient?) = apply {
            this.okHttpClient = okHttpClient
        }

        fun setIsLogOutPut(isLogOutPut: Boolean) = apply {
            this.isLogOutPut = isLogOutPut
        }

        fun setLoadingTitle(loadingTitle: String?) = apply {
            this.loadingTitle = loadingTitle
        }

        fun setDefaultHeader(defaultHeader: HashMap<String, String>?) = apply {
            this.defaultHeader = defaultHeader
        }

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

        fun setDelaysProcessLimitTimeMillis(delaysProcessLimitTimeMillis: Long) = apply {
            this.delaysProcessLimitTimeMillis = delaysProcessLimitTimeMillis
        }

        fun setSpecifiedTimeoutMillis(specifiedTimeoutMillis: Long) = apply {
            this.specifiedTimeoutMillis = specifiedTimeoutMillis
        }

        fun setJsonCovertKey(messageKey: String = MESSAGE_KEY,
                             codeKey: String = CODE_KEY,
                             dataKey: String = DATA_KEY,
                             successCode: Int = OK_CODE) = apply {
            this.messageKey = messageKey
            this.codeKey = codeKey
            this.dataKey = dataKey
            this.successCode = successCode
        }

        fun build(): HttpBuilder {
            return HttpBuilder(this)
        }
    }

    companion object {
        @JvmStatic
        fun create(activity: FragmentActivity) = Builder(activity)

        @JvmStatic
        fun getDefaultBuilder(activity: FragmentActivity): HttpBuilder {
            return create(activity)
                .setLoadingDialog(HttpLoadingDialog.defaultDialog)
                .build()
        }
    }
}