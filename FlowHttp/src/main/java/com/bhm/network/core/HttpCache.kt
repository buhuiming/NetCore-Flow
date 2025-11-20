package com.bhm.network.core

import android.content.Context
import android.webkit.WebSettings
import okhttp3.Cache
import java.io.File

/**
 * Created by bhm on 2023/5/6.
 */
object HttpCache {

    private const val HTTP_RESPONSE_DISK_CACHE_MAX_SIZE = 50 * 1024 * 1024

    @JvmStatic
    fun getCache(context: Context): Cache {
        return Cache(
            File(context.applicationContext.cacheDir.absolutePath + File.separator + "data/NetCache"),
            HTTP_RESPONSE_DISK_CACHE_MAX_SIZE.toLong()
        )
    }

    /**
     * 获取UserAgent
     * @return UserAgent
     */
    @JvmStatic
    fun getUserAgent(context: Context?): String {
        val userAgent =
            try {
                WebSettings.getDefaultUserAgent(context)
            } catch (e: Exception) {
                System.getProperty("http.agent")?: ""
            }
        val sb = StringBuilder()
        var i = 0
        val length = userAgent.length
        while (i < length) {
            val c = userAgent[i]
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", c.code))
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}