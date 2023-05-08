package com.bhm.network.core

import com.bhm.network.core.callback.CommonCallBack
import com.bhm.network.core.callback.DownloadCallBack
import com.bhm.network.core.callback.UploadCallBack
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
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = CommonCallBack<E>()
            call.apply(callBack)
            return httpOptions.enqueue(api, httpCall, call)
        }

        /**
         * 执行上传请求
         */
        fun <T : Any> uploadExecute(aClass: Class<T>, httpCall: suspend (T) -> E, callBack: UploadCallBack<E>.() -> Unit): Job {
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = UploadCallBack<E>()
            call.apply(callBack)
            return httpOptions.uploadEnqueue(api, httpCall, call)
        }

        /**
         * 执行下载请求
         */
        fun <T : Any> downloadExecute(aClass: Class<T>, httpCall: suspend (T) -> ResponseBody, callBack: DownloadCallBack.() -> Unit): Job {
            val api = RetrofitHelper(httpOptions).createRequest(aClass, baseUrl)
            val call = DownloadCallBack()
            call.apply(callBack)
            return httpOptions.downloadEnqueue(api, httpCall, call)
        }
    }
}