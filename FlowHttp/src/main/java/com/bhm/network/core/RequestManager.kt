package com.bhm.network.core

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bhm.network.core.callback.CallBackImp
import com.bhm.network.core.callback.CommonCallBack
import com.bhm.network.core.callback.DownloadCallBack
import com.bhm.network.core.callback.UploadCallBack
import com.bhm.network.define.CommonUtil
import com.bhm.network.define.ResultException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.util.concurrent.TimeoutException

/**
 * @author Buhuiming
 * @description:
 * @date :2023/5/6
 */
@Suppress("SENSELESS_COMPARISON")
class RequestManager private constructor() {

    companion object {

        private var instance: RequestManager = RequestManager()

        @JvmStatic
        fun get(): RequestManager {
            synchronized(RequestManager::class.java) {
                if (instance == null) {
                    instance = RequestManager()
                }
            }
            return instance
        }
    }

    /**
     * 取消请求
     */
    fun removeJob(job: Job?) {
        JobManager.get().removeJob(job)
    }

    fun <E : Any> buildRequest(): Manager<E> {
        return Manager()
    }

    class Manager<E : Any> {

        private lateinit var httpOptions: HttpOptions

        private lateinit var baseUrl: String

        /**
         * 设置请求属性
         */
        fun setHttpOptions(httpOptions: HttpOptions) = apply {
            this.httpOptions = httpOptions
        }

        /**
         * 设置URL域名
         */
        fun setBaseUrl(url: String) = apply {
            baseUrl = url
        }

        /**
         * 执行请求
         */
        fun <T : Any> execute(aClass: Class<T>, httpCall: suspend (T) -> E, callBack: CommonCallBack<E>.() -> Unit): Job {
            checkOptions()
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = CommonCallBack<E>()
            call.apply(callBack)
            return enqueue(api, httpCall, call)
        }

        /**
         * 执行上传请求
         */
        fun <T : Any> uploadExecute(aClass: Class<T>, httpCall: suspend (T) -> E, callBack: UploadCallBack<E>.() -> Unit): Job {
            checkOptions()
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = UploadCallBack<E>()
            call.apply(callBack)
            return uploadEnqueue(api, httpCall, call)
        }

        /**
         * 执行下载请求
         */
        fun <T : Any> downloadExecute(aClass: Class<T>, httpCall: suspend (T) -> ResponseBody, callBack: DownloadCallBack.() -> Unit): Job {
            checkOptions()
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = DownloadCallBack()
            call.apply(callBack)
            return downloadEnqueue(api, httpCall, call)
        }

        private fun checkOptions() {
            requireNotNull(httpOptions) { "Please initialize HttpOptions" }
        }

        /**
         * 设置请求回调
         */
        private fun <T: Any, E: Any> enqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
            httpOptions.callBack = callBack
            val job = CoroutineScope(Dispatchers.IO).launch {
                httpOptions.currentRequestDateTamp = System.currentTimeMillis()
                flow {
                    emit(httpCall(api))
                }
                    .catch {
                        CommonUtil.logger(
                            httpOptions,
                            "ThrowableConsumer-> ",
                            it.message
                        ) //抛异常
                        val requestSpentTime = System.currentTimeMillis() - httpOptions.currentRequestDateTamp
                        if (requestSpentTime < httpOptions.delaysProcessLimitTimeMillis) {
                            delay(httpOptions.delaysProcessLimitTimeMillis - requestSpentTime)
                            doThrowableConsumer(callBack, it)
                        } else {
                            doThrowableConsumer(callBack, it)
                        }
                        JobManager.get().removeJob(httpOptions.jobKey)
                    }
                    .onStart {
                        callBack?.onStart(httpOptions.specifiedTimeoutMillis)
                    }
                    .onCompletion {
                        callBack?.onComplete()
                    }
                    .flowOn(Dispatchers.Main)
                    .collect {
                        if (isActive) {
                            val requestSpentTime = System.currentTimeMillis() - httpOptions.currentRequestDateTamp
                            if (requestSpentTime < httpOptions.delaysProcessLimitTimeMillis) {
                                delay(httpOptions.delaysProcessLimitTimeMillis - requestSpentTime)
                                doBaseConsumer(callBack, it)
                            } else {
                                doBaseConsumer(callBack, it)
                            }
                        }
                        JobManager.get().removeJob(httpOptions.jobKey)
                    }
            }
            JobManager.get().addJob(httpOptions.jobKey, job)
            return job
        }

        /*
        *  设置上传文件回调
        */
        private fun <T: Any, E: Any> uploadEnqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
            return this.enqueue(api, httpCall, callBack)
        }

        /*
        *  设置文件下载回调
        */
        private fun <T: Any, E: Any> downloadEnqueue(api: T, httpCall: suspend (T) -> E, callBack: CallBackImp<E>?): Job {
            httpOptions.callBack = callBack
            val job = CoroutineScope(Dispatchers.IO).launch {
                httpOptions.currentRequestDateTamp = System.currentTimeMillis()
                flow {
                    emit(httpCall(api))
                }
                    .flowOn(Dispatchers.IO)
                    .catch {
                        callBack?.onFail(it)
                        if (null != httpOptions.dialog && httpOptions.isShowDialog) {
                            httpOptions.dialog?.dismissLoading(httpOptions.activity)
                        }
                        JobManager.get().removeJob(httpOptions.jobKey)
                    }
                    .onStart {
                        callBack?.onStart(httpOptions.specifiedTimeoutMillis)
                    }
                    .onCompletion {
                        callBack?.onComplete()
                    }
                    .flowOn(Dispatchers.Main)
                    .collect {
                        val requestSpentTime = System.currentTimeMillis() - httpOptions.currentRequestDateTamp
                        if (requestSpentTime < httpOptions.delaysProcessLimitTimeMillis) {
                            delay(httpOptions.delaysProcessLimitTimeMillis - requestSpentTime)
                            doBaseConsumer(callBack, it)
                        } else {
                            doBaseConsumer(callBack, it)
                        }
                        JobManager.get().removeJob(httpOptions.jobKey)
                    }
            }
            JobManager.get().addJob(httpOptions.jobKey, job)
            return job
        }

        private fun <E: Any> doBaseConsumer(callBack: CallBackImp<E>?, t: E) {
            if (httpOptions.isDialogDismissInterruptRequest) {
                httpOptions.activity.lifecycleScope.launch(Dispatchers.Main) {
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
            if (httpOptions.isShowDialog && null != httpOptions.dialog) {
                httpOptions.dialog?.dismissLoading(httpOptions.activity)
            }
        }

        private fun <T: Any> doThrowableConsumer(callBack: CallBackImp<T>?, e: Throwable) {
            if (httpOptions.isDialogDismissInterruptRequest) {
                httpOptions.activity.lifecycleScope.launch(Dispatchers.Main) {
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
            if (httpOptions.isShowDialog && null != httpOptions.dialog) {
                httpOptions.dialog?.dismissLoading(httpOptions.activity)
            }
            if (httpOptions.isDefaultToast) {
                when (e) {
                    is HttpException -> {
                        when {
                            e.code() == 404 -> {
                                Toast.makeText(httpOptions.activity, e.message, Toast.LENGTH_SHORT).show()
                            }
                            e.code() == 504 -> {
                                Toast.makeText(httpOptions.activity, "请检查网络连接！", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Toast.makeText(httpOptions.activity, "请检查网络连接！", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    is IndexOutOfBoundsException, is NullPointerException, is JsonSyntaxException, is IllegalStateException, is ResultException -> {
                        Toast.makeText(httpOptions.activity, "数据异常，解析失败！", Toast.LENGTH_SHORT).show()
                    }

                    is TimeoutException -> {
                        Toast.makeText(httpOptions.activity, "连接超时，请重试！", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        Toast.makeText(httpOptions.activity, "请求失败，请稍后再试！", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}