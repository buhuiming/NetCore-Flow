@file:Suppress("unused", "SENSELESS_COMPARISON")

package com.bhm.network.core

import android.util.Log
import kotlinx.coroutines.Job

/**
 * Created by bhm on 2023/5/6.
 * 用于管理每个请求
 */
internal class JobManager private constructor() {

    private var jobMap: HashMap<String, Job> = HashMap(1)

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

    fun addJob(key: String, job: Job) {
        if (jobMap.containsValue(job)) {
            removeJob(job)
        }
        jobMap[key] = job
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
            Log.d("JobManager", "remove JobTask by key")
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
            Log.d("JobManager", "remove JobTask by job")
        }
    }

    private fun isExitJob(job: Job?): Boolean {
        return jobMap.containsValue(job)
    }
}