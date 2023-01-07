package com.uz.kitoblar.utils

import android.app.Notification
import android.net.Uri
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.PlatformScheduler
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.uz.kitoblar.applicationContext
import com.uz.kitoblar.ui.toast
import java.io.File
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object DownloadController {
    lateinit var databaseProvider: DatabaseProvider
    lateinit var simpleCache: SimpleCache
    private lateinit var downloadDirectory: File
    lateinit var httpDataSource: HttpDataSource.Factory
    lateinit var executor: Executor
    lateinit var downloadManager: DownloadManager
    lateinit var cacheMediaSourceFactory: DefaultMediaSourceFactory

    private const val cacheFileName = "books music"

    private var created = false

    fun create() {
        if (created) return
        created = true
        val context = applicationContext()
        databaseProvider = StandaloneDatabaseProvider(applicationContext())
        var path = context.getExternalFilesDir(null)
        if (path == null) {
            path = context.filesDir
        }
        downloadDirectory = File(path, cacheFileName)
        simpleCache = SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
        CookieHandler.setDefault(cookieManager)

        httpDataSource = DefaultHttpDataSource.Factory()
        executor = Executors.newSingleThreadExecutor()

        val cacheDataSource = CacheDataSource.Factory().apply {
            setCache(simpleCache)
            setUpstreamDataSourceFactory(httpDataSource)
            setCacheWriteDataSinkFactory(null)
        }
        cacheMediaSourceFactory = DefaultMediaSourceFactory(applicationContext()).apply {
            setDataSourceFactory(cacheDataSource)
        }
        downloadManager = DownloadManager(
            context, databaseProvider, simpleCache, httpDataSource,
            executor
        ).apply {
            maxParallelDownloads = 1

            addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?
                ) {
                    super.onDownloadChanged(downloadManager, download, finalException)

                    if (finalException != null) {
                        toast(finalException.message)
                        return
                    }
                    listeners.forEach {
                        if (it.key == download.request.id) {
                            it.value.onProgressChanges(downloadManager, download)
                        }
                    }
                }
            })
        }
    }

    private fun configureDownloads() {
        downloadManager.currentDownloads.forEach {
            if (it.state == Download.STATE_FAILED) {
                stopDownload(it.request.id)
            }
        }
    }

    fun checkOffline(bookId: String): Boolean {
        val d = getDownload(bookId)
        return d?.state == Download.STATE_COMPLETED && d.bytesDownloaded > 0L
    }

    fun download(url: String): Boolean {
        configureDownloads()
        val hasPending =
            downloadManager.currentDownloads.find { it.state == Download.STATE_DOWNLOADING } != null
        if (hasPending) {
            return false
        }
        val downloadRequest = DownloadRequest.Builder(url, Uri.parse(url)).build()
        DownloadService.sendAddDownload(
            applicationContext(),
            MyDownloadService::class.java,
            downloadRequest,
            false
        )
        return true
    }

    fun pauseAllDownloads() {
        DownloadService.sendPauseDownloads(
            applicationContext(),
            MyDownloadService::class.java,
            false
        )
    }

    fun stopDownload(url: String) {
        configureDownloads()
        DownloadService.sendRemoveDownload(
            applicationContext(),
            MyDownloadService::class.java,
            url,
            false
        )
    }

    fun resumeDownloads() {
        DownloadService.sendResumeDownloads(
            applicationContext(),
            MyDownloadService::class.java,
            false
        )
    }

    fun pauseDownloads() {
        DownloadService.sendPauseDownloads(applicationContext(), DownloadService::class.java, false)
    }

    fun getDownload(id: String): Download? {
        configureDownloads()
        try {
            return downloadManager.downloadIndex.getDownload(id)
        } catch (e: Exception) {

        }
        return null
    }

    fun isDownloading(url: String): Boolean {
        configureDownloads()
        return getDownload(url)?.state == Download.STATE_DOWNLOADING
    }

    interface DownloadProgressCallback {
        fun onProgressChanges(manager: DownloadManager, download: Download)
    }

    private var listeners = hashMapOf<String, DownloadProgressCallback>()

    fun setListener(id: String, callback: DownloadProgressCallback) {
        listeners[id] = callback
    }

    fun removeListener(id: String) {
        listeners.remove(id)
    }

}

class MyDownloadService : DownloadService(0) {
    private val jobId = 0

    override fun getDownloadManager(): DownloadManager {
        DownloadController.create()
        return DownloadController.downloadManager
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(applicationContext, jobId)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return Notification()
    }

}