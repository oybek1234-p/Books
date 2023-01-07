package com.uz.kitoblar.ui.controllers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Build
import android.widget.SeekBar
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.MusicDialogBinding
import com.uz.kitoblar.ui.components.actionBar.AlertsCreator
import com.uz.kitoblar.ui.fragments.DetailsFragment
import com.uz.kitoblar.ui.fragments.MobileNumberFragment
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.DownloadController
import kotlin.time.DurationUnit
import kotlin.time.toDuration

interface MusicInterface {
    fun setMusic(url: String)
    fun setCallback(playCallback: MusicPlayCallback)
    fun play()
    fun pause()
    fun stop()
    fun loading(b: Boolean)
}

interface MusicPlayCallback {
    fun onUpdate(value: Float, currentPos: String, duration: String)
}

class MusicController(private val musicView: MusicDialogBinding) : MusicInterface {

    private var mediaPlayer: ExoPlayer? = null
    private var musicPlayCallback: MusicPlayCallback? = null
    private var descriptionAdapter = DescriptionAdapter()
    private val channelId = "bookAppId"
    private val notificationId = 772
    var buttonColor = 0

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "BookApp"
            val descriptionText = "Book app listening";
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(channelId, name, importance)
            mChannel.setSound(null, null)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager =
                applicationContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    init {
        createNotificationChannel()
    }

    private var playerNotificationManager =
        PlayerNotificationManager.Builder(
            applicationContext(),
            notificationId,
            channelId
        ).setMediaDescriptionAdapter(descriptionAdapter).build()

    var currentPlayingUrl = ""

    private var musicDuration = 0L
    private var currentDuration = 0L
    private var progress = 0f
    var currentBook = Book()

    var callback: MusicInterface? = null

    fun playing() = play
    private var play = false

    private var animating = false

    private var showedMusicView = false

    inner class DescriptionAdapter : PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return currentBook.name
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    applicationContext(),
                    0,
                    Intent(
                        applicationContext(),
                        MainActivity::class.java
                    ).setAction(currentDate().toString()),
                    PendingIntent.FLAG_MUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    applicationContext(),
                    0,
                    Intent(
                        applicationContext(),
                        MainActivity::class.java
                    ).setAction(currentDate().toString()), PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return currentBook.authorName
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            try {
                val bookPhoto = currentBook.photo
                launchOnBack {
                    val future =
                        Glide.with(musicView.root.context).asBitmap().load(bookPhoto).submit()
                    val bitmap = future.get()
                    launchOnMain {
                        callback.onBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {

            }
            return null
        }
    }

    fun showMusicView(show: Boolean) {
        if (showedMusicView == show) return
        showedMusicView = show
        if (show) {
            setUpMusicView()
        }
        musicView.root.apply {
            if (animating) {
                animate().cancel()
            }
            if (show == isVisible) return
            visibleOrGone(true)
            animate().alpha(if (show) 1f else 0f).translationY(if (show) 0f else height.toFloat())
                .setDuration(300)
                .withEndAction {
                    animating = false
                    if (!show) {
                        visibleOrGone(false)
                    }
                }.withStartAction {
                    animating = true
                }
                .setInterpolator(decelerateInterpolator).start()
        }
    }

    fun setBook(book: Book) {
        if (book == currentBook) return
        if (mediaPlayer != null) {
            stop()
        }
        currentBook = book
        musicView.apply {
            imageView.setData(book.photo)
            textView13.text = book.name
            infoView.text = book.getInfo(true)
            setMusic(book.musicUrl)
        }
    }

    private fun updatePlayIcon() {
        musicView.playView.apply {
            if (play) {
                setImageResource(R.drawable.ic_pause)
//                if (backgroundTintList?.defaultColor != buttonColor) {
//                    backgroundTintList = ColorStateList.valueOf(buttonColor)
//                }
            } else {
                setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun setUpMusicView() {
        musicView.apply {
            root.setOnClickListener {
                if (currentBook.id.isNotEmpty()) {
                    findActivity(root.context)?.openFragment(DetailsFragment(currentBook.id), false)
                }
            }
            playView.setOnClickListener {
                if (play) {
                    pause()
                } else {
                    play()
                }
            }
            exitButton.setOnClickListener {
                stop()
            }
            seekBar.apply {
                setCallback(object : MusicPlayCallback {
                    override fun onUpdate(value: Float, currentPos: String, duration: String) {
                        progress = value.toInt()
                        timeView.text = "$currentPos / $duration"
                    }
                })
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        pause()
                    }

                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        seekToProgress(progress)
                        play()
                    }
                })
            }
        }
    }

    companion object {

        fun getMusicDuration(context: Context, uri: String, result: (duration: Long) -> Unit) {
            val mediaPlayer = ExoPlayer.Builder(context).build()
            mediaPlayer.setMediaItem(MediaItem.fromUri(uri))
            mediaPlayer.prepare()

            mediaPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    if (playbackState == ExoPlayer.STATE_READY) {
                        result.invoke(mediaPlayer.contentDuration)
                    }
                    mediaPlayer.release()
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    mediaPlayer.release()
                }
            })
        }

        fun checkMp3Url(url: String) = url.endsWith(".mp3")

        fun askForLoginToSave(context: Context) {
            if (userLogged()) return
            AlertsCreator.showAlert(
                context,
                "Kitobni davom ettirish",
                "Audio kitobni keyinchalik davom ettirish uchun akkauntga kiring!",
                "Hozir emas",
                "Akkauntga kirish", {

                }, {
                    findActivity(context)?.openFragment(MobileNumberFragment(), false)
                }, imageResource = R.drawable.ic_bookmark
            )
        }
    }

    private var progressRunnable = Runnable {
        mediaPlayer?.apply {

            val loading = isLoading && !isPlaying
            callback?.loading(loading)

            if (play) {
                currentDuration = currentPosition
                musicDuration = if (currentBook.duration > 1L) {
                    currentBook.duration
                } else {
                    contentDuration
                }
                progress = (currentDuration.toFloat() / musicDuration.toFloat() * 100f)
                val currentPosText = (currentDuration.toFloat() / 1000 / 60f).toInt()
                    .toDuration(DurationUnit.MINUTES).toString(DurationUnit.MINUTES)

                val durationText =
                    (musicDuration.toFloat() / 1000f / 60f).toInt().toDuration(DurationUnit.MINUTES)
                        .toString(DurationUnit.MINUTES)
                musicPlayCallback?.onUpdate(progress, currentPosText, durationText)

                updateProgress()
            }
        }
    }

    private fun cancelProgress() {
        cancelRunOnUiThread(progressRunnable)
        progress = 0f
    }

    private fun updateProgress() {
        if (progress > 98f) {
            seekToProgress(0)
            pause()
            return
        }
        cancelProgress()
        if (mediaPlayer == null) {
            cancelProgress()
        } else {
            runOnUiThread(progressRunnable, 100)
        }
    }

    private fun initPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer =
                ExoPlayer.Builder(musicView.root.context)
                    .setMediaSourceFactory(DownloadController.cacheMediaSourceFactory)
                    .setUseLazyPreparation(true).build()

            playerNotificationManager.setPriority(NotificationCompat.PRIORITY_MAX)
            playerNotificationManager.setPlayer(mediaPlayer)

      //      seekToProgress()
            setUpMusicView()
        }
    }

    fun getTimeText(book: Book): String {
        val myBook = booksController().myBooks.find { it.id == book.id }
        var currentPosText = "0"
        if (myBook != null) {
            currentPosText = (myBook.musicDurationDone.toFloat() / 1000 / 60f).toInt()
                .toDuration(DurationUnit.MINUTES).toString(DurationUnit.MINUTES)
        }
        val durationText =
            (book.duration.toFloat() / 1000f / 60f).toInt().toDuration(DurationUnit.MINUTES)
                .toString(DurationUnit.MINUTES)
        return "$currentPosText / $durationText"
    }

    fun getBookProgress(book: Book): Int {
        val myBook = booksController().myBooks.find { it.id == book.id }
        if (myBook != null) {
            val current = myBook.musicDurationDone
            val all = book.duration
            val p = current.toFloat() / all.toFloat() * 100f
            if (currentBook.id == book.id) {
                val cp = mediaPlayer?.currentPosition ?: 0
                val curPr = cp.toFloat() / all.toFloat() * 100f
                return curPr.toInt()
            }
            return p.toInt()
        }
        return 0
    }

    override fun setMusic(url: String) {
        if (url.isEmpty() && !checkMp3Url(url)) return
        if (url == currentPlayingUrl) return

        currentPlayingUrl = url

        initPlayer()

        mediaPlayer!!.apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            musicDuration = currentBook.duration
            val myBook = booksController().myBooks.find { it.id == currentBook.id }
            if (myBook != null) {
                val seek = myBook.musicDurationDone
                seekTo(seek)
            } else {
                seekTo(0)
            }
            updateProgress()
            prepare()
        }
    }

    override fun setCallback(playCallback: MusicPlayCallback) {
        this.musicPlayCallback = playCallback
    }

    fun seekToProgress(progress: Int) {
        cancelProgress()
        var seekTo = 0f
        if (progress > 0) {
            seekTo = (progress.toFloat() / 100f) * musicDuration
        }
        mediaPlayer?.seekTo(seekTo.toLong())
        updateProgress()
    }

    override fun play() {
        if (play) return
        play = true

        if (mediaPlayer == null && currentPlayingUrl.isNotEmpty()) {
            initPlayer()
            setMusic(currentPlayingUrl)
        }
        updateProgress()
        addMyBook()
        mediaPlayer?.play()
        updatePlayIcon()
        callback?.play()
    }

    override fun pause() {
        play = false
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            //
        }
        cancelProgress()
        updatePlayIcon()
        callback?.pause()
    }

    private fun addMyBook() {
       // booksController().addBookToLibrary(currentBook) {}
    }

    private fun updateBook() {
        val id = currentBook.id
        if (id.isNotEmpty()) {
            val myBook = booksController().myBooks.find { it.id == id }
            if (myBook != null) {
                myBook.musicDurationDone = currentDuration
                myBook.lastReadTime = currentDuration
                booksController().updateBook(myBook) {}
            }
        }
    }

    override fun stop() {
        if (play) {
            if (!userLogged()) {
                askForLoginToSave(musicView.root.context)
            }
        }
        play = false
        updateBook()
        try {
            playerNotificationManager.setPlayer(null)
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            //
        }
        showMusicView(false)
        cancelProgress()
        currentPlayingUrl = ""
        currentBook = Book()
        mediaPlayer = null
        callback?.stop()

    }

    fun seekToMillis(millis: Long) {
        mediaPlayer?.seekTo(millis)
    }

    override fun loading(b: Boolean) {

    }
}