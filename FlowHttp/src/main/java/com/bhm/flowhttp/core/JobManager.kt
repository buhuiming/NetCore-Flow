@file:Suppress("unused")

package com.bhm.flowhttp.core

import kotlinx.coroutines.Job

/**
 * Created by bhm on 2023/5/6.
 * 用于管理每个请求
 */
class JobManager {

    private var list: MutableList<Job>? = ArrayList()

    fun add(job: Job) {
        if (null == list) {
            list = ArrayList()
        }
        if (list?.contains(job) == true) {
            removeJob(job)
        }
        list?.add(job)
    }

    /**
     * 清空监听，再次调用需new CompositeDisposable()
     */
    fun clear() {
        list?.clear()
        list = null
    }

    /**
     * 取消一个请求
     */
    fun removeJob() { //中断监听 取消请求
        list?.let {
            if (it.size > 0) {
                val job = it[it.size - 1]
                it.remove(job)
                job.cancel()
            }
        }
    }

    /**
     * 取消一个请求
     */
    fun removeJob(job: Job?) { //中断监听 取消请求
        job?.let {
            if (isExitJob(it)) {
                list?.remove(it)
                job.cancel()
            }
        }
    }

    private fun isExitJob(job: Job): Boolean {
        return list?.contains(job)?: false
    }
}