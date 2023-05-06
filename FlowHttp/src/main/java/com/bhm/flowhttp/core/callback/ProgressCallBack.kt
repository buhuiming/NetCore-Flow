package com.bhm.flowhttp.core.callback

/**
 * @author Buhuiming
 * @description:进度回调
 * @date :2023/5/6
 */
abstract class ProgressCallBack<T>: SpecifiedTimeoutCallBack<T>() {
    abstract fun onProgress(progress: Int, bytesWritten: Long, contentLength: Long)
}