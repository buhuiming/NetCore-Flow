package com.bhm.flowhttp.core

import com.bhm.flowhttp.core.callback.CommonCallBack
import com.bhm.flowhttp.core.callback.DownloadCallBack
import com.bhm.flowhttp.core.callback.UploadCallBack
import kotlinx.coroutines.Job
import okhttp3.ResponseBody

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

    fun removeJob(job: Job?) {
        JobManager.get().removeJob(job)
    }

    fun <E : Any> callManager(): Manager<E> {
        return Manager()
    }

    class Manager<E : Any> {

        private lateinit var httpBuilder: HttpBuilder

        private lateinit var baseUrl: String

        fun setHttpBuilder(httpBuilder: HttpBuilder) = apply {
            this.httpBuilder = httpBuilder
        }

        fun setBaseUrl(url: String) = apply {
            baseUrl = url
        }

        fun <T : Any> execute(aClass: Class<T>, httpCall: suspend (T) -> E, callBack: CommonCallBack<E>.() -> Unit): Job {
            val api = RetrofitHelper(httpBuilder).createRequest(aClass, baseUrl)
            val call = CommonCallBack<E>()
            call.apply(callBack)
            return httpBuilder.enqueue(api, httpCall, call)
        }

        fun <T : Any> uploadEnqueue(aClass: Class<T>, httpCall: suspend (T) -> E, callBack: UploadCallBack<E>.() -> Unit): Job {
            val api = RetrofitHelper(httpBuilder).createRequest(aClass, baseUrl)
            val call = UploadCallBack<E>()
            call.apply(callBack)
            return httpBuilder.uploadEnqueue(api, httpCall, call)
        }

        fun <T : Any> downloadExecute(aClass: Class<T>, httpCall: suspend (T) -> ResponseBody, callBack: DownloadCallBack.() -> Unit): Job {
            val api = RetrofitHelper(httpBuilder).createRequest(aClass, baseUrl)
            val call = DownloadCallBack()
            call.apply(callBack)
            return httpBuilder.downloadEnqueue(api, httpCall, call)
        }
    }
}