package com.uz.kitoblar

import android.os.Environment
import com.ixuea.android.downloader.DownloadService
import com.ixuea.android.downloader.callback.DownloadListener
import com.ixuea.android.downloader.domain.DownloadInfo
import com.uz.kitoblar.ui.toast
import java.io.File

class PdfController {
    companion object {
        const val ENDING_TAG = ".pdf"

        private var instance: PdfController? = null

        fun getInstance(): PdfController {
            return instance ?: PdfController().also { instance = it }
        }
    }

    private var downloadManager: com.ixuea.android.downloader.callback.DownloadManager =
        DownloadService.getDownloadManager(
            applicationContext()
        )

    private var cDownloadInfo: DownloadInfo? = null
    private var currentDownloadingUrl = ""

    fun getPdf(pdfUrl: String): String? {
        val downloadInfo = downloadManager.getDownloadById(pdfUrl)
        if (downloadInfo == null) {
            return null
        } else {
            if (downloadInfo.progress == downloadInfo.size) {
                return downloadInfo.path
            }
        }
        return null
    }

    fun deletePdf(url: String) {
        val info = downloadManager.getDownloadById(url)
        if (info != null) {
            downloadManager.remove(info)
        }
    }

    fun getProgress(url: String) : Int {
        downloadManager.getDownloadById(url)?.apply {
            val p = (progress.toFloat() / size.toFloat() * 100f).toInt()
            return p
        }
        return 0
    }

    fun cancelDownload(url: String) {
        val info = downloadManager.getDownloadById(url) ?: return
        downloadManager.pause(info)
    }

    fun downloadPdf(url: String, downloadListener: DownloadListener) {
        //cancelDownload(url)
        currentDownloadingUrl = url
        val path = url.replace("/","")
        val file =
            File(applicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), path)
        if (!file.exists()) {
            file.createNewFile()
        }
        val downloadInfo = DownloadInfo.Builder().setId(url).setPath(file.path).setUrl(url)
            .build()
        downloadInfo.downloadListener = downloadListener
        cDownloadInfo = downloadInfo
        downloadManager.download(downloadInfo)
    }
}