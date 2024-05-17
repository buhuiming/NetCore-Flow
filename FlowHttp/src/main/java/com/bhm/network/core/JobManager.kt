@file:Suppress("unused", "SENSELESS_COMPARISON")

package com.bhm.network.core

import com.bhm.network.define.CommonUtil
import kotlinx.coroutines.Job

/**
 * Created by bhm on 2023/5/6.
 * 用于管理每个请求
 */
internal class JobManager private constructor() {

    private var jobMap: HashMap<String, Job> = HashMap(1)

    private var httpOptions: HttpOptions? = null

    companion object {

        private var instance: JobManager = JobManager()

        @JvmStatic
        fun get(): JobManager {
            synchronized(JobManager::class.java) {
                if (instance == null) {
                    instance = JobManager()
                }
            }
            return instance
        }
    }

    fun setHttpOptions(httpOptions: HttpOptions) {
        this.httpOptions = httpOptions
    }

    fun addJob(key: String, job: Job) {
        if (jobMap.containsValue(job)) {
            removeJob(job)
        }
        jobMap[key] = job
        CommonUtil.logger(httpOptions, javaClass.simpleName, "添加key=$key Http请求任务")
    }

    fun clear() {
        jobMap.clear()
    }

    /**
     * 取消一个请求
     */
    fun removeJob(key: String) { //中断监听 取消请求
        if (jobMap.isNotEmpty()) {
            val job = jobMap[key]
            jobMap.remove(key)
            job?.cancel()
            CommonUtil.logger(httpOptions, javaClass.simpleName, "通过key=$key 移除Http请求任务")
        }
    }

    /**
     * 取消一个请求
     */
    fun removeJob(job: Job?) { //中断监听 取消请求
        if (isExitJob(job)) {
            val iterator = jobMap.iterator()
            while (iterator.hasNext()) {
                val obj = iterator.next()
                if (obj.value == job) {
                    iterator.remove()
                }
            }
            job?.cancel()
            CommonUtil.logger(httpOptions, javaClass.simpleName, "通过Job移除Http请求任务")
        }
    }

    private fun isExitJob(job: Job?): Boolean {
        return jobMap.containsValue(job)
    }
}