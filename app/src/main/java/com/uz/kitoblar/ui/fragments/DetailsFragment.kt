package com.uz.kitoblar.ui.fragments

import am.leon.Media
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import android.widget.SeekBar
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.databinding.ViewDataBinding
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.ixuea.android.downloader.callback.DownloadListener
import com.ixuea.android.downloader.exception.DownloadException
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.*
import com.uz.kitoblar.ui.components.actionBar.*
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase
import com.uz.kitoblar.ui.controllers.*
import com.uz.kitoblar.ui.inflateBinding
import com.uz.kitoblar.ui.playingLottie
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.DownloadController
import com.uz.kitoblar.utils.formatCurrency
import java.io.File


class DetailsFragment(private val bookId: String, private val play: Boolean = true) :
    BaseFragment<DetailsFragmentBinding>(R.layout.details_fragment) {
    private var book = Book()
    private lateinit var mAdapter: BooksFragment.InnerAdapter
    private var moreList = arrayListOf<Book>()
    private var liked = false

    private var pauseDrawable = getDrawable(R.drawable.ic_pause)
    private var playDrawable = getDrawable(R.drawable.baseline_play_arrow_black_24)

    private var palette: Palette? = null

    companion object {
        fun showDescriptionForBook(activity: MainActivity, book: Book) {
            val sheet =
                object : BottomSheetBase<DescriptionLayoutBinding>(R.layout.description_layout) {
                    override fun onViewCreated(binding: DescriptionLayoutBinding) {
                        super.onViewCreated(binding)
                        container().apply {
                            titleView.text = "Malumot"
                        }
                        binding.apply {
                            textView.text = book.about
                        }
                    }
                }
            activity.sheetController.showSheet(sheet)
        }
    }

    private fun isDarkColor(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    private fun lightenColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.95f)
    }

    private var backColorS = 0
    private var textColorS = 0
    private var buttonBackS = 0

    fun updateColors() {
        updateLikeView()
        palette?.apply {
            requireBinding().apply {
                val swatch = darkVibrantSwatch ?: vibrantSwatch ?: lightVibrantSwatch ?: mutedSwatch
                swatch?.let {
                    requireActionBar().apply {
                        lightVibrantSwatch?.let {
                            backColorS = it.rgb
                            textColorS = it.titleTextColor
                        }
                        backgroundColor = it.rgb
                        iconsColor = lightenColor(it.titleTextColor)
                        bookPartsAdapter?.notifyDataSetChanged()
                        vibrantSwatch?.let {
                            readButton.apply {
                                backgroundTintList =
                                    ColorStateList.valueOf(it.rgb.also { buttonBackS = it }).apply {
                                        playButton.compoundDrawableTintList = this
                                    }
                                player().buttonColor = buttonBackS
                                setTextColor(lightenColor(it.bodyTextColor))
                            }
                        }
                        val backDark = isDarkColor(it.rgb)
                        mainActivity().setLightStatusBar((!backDark).also { lightStatusBar = it })
                    }
                }
            }
        }
    }

    private var bookParts = arrayListOf<Book>()
    private var bookPartsLoading = false
    private var bookPartsAdapter: BooksFragment.InnerAdapter? = null

    fun updateBookParts(animate: Boolean = true) {
        if (bookParts.isNotEmpty()) {
            bookPartsAdapter?.updateList(bookParts)
            requireBinding().apply {
                val index = bookParts.indexOfFirst { it.id == book.id }
                partsRecyclerView.smoothScrollToPosition(index)
                qismlarTitleView.apply {
                    text = "${bookParts.size}ta qism"
                    visibleOrGone(true, if (animate) 1 else 0)
                }
                partsRecyclerView.visibleOrGone(true, if (animate) 1 else 0)
            }
        }
    }

    private fun loadBookParts() {
        bookPartsLoading = true
        booksController().loadBooksParts(book.parentBook, object : Result<ArrayList<Book>>() {
            override fun onResult(result: ArrayList<Book>) {
                bookParts = result.apply {
                    sortByDescending { it.part }
                    reverse()
                }
                bookPartsLoading = false
                updateBookParts()
            }
        })
    }

    fun updatePlayView(play: Boolean) {
        //  playing = play
        requireBinding().apply {
            playButton.apply {
                // text = if (play) "Pause" else "Play"
                setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    if (play) pauseDrawable else playDrawable,
                    null,
                    null
                )
            }
        }
    }

    private var likeActive = 0
    private var likeNotActive = 0

    private var likeLoading = false

    private fun checkLiked() {
        likeLoading = true
        booksController().checkBookLiked(bookId) {
            likeLoading = false
            liked = it
            updateLikeView()
        }
    }

    private fun doLike() {
        if (!userLogged()) {
            runOnUiThread({
                openFragment(MobileNumberFragment())
            }, 200)
            return
        }
        if (likeLoading) {
            return
        }
        liked = !liked
        if (liked) {
            book.likes += 1
        } else {
            book.likes -= 1
        }
        updateLikeView()
        likeLoading = true
        booksController().likeBook(book, liked) {
            likeLoading = false
        }
    }

    private fun updateLikeView() {
        requireBinding().heartView.apply {
            if (!isVisible) {
                visibleOrGone(true, 1)
            }
            text = book.likes.toString() + " ta yoqdi"
            compoundDrawables.forEach {
                it?.setTint(if (liked) likeActive else likeNotActive)
            }
        }
    }

    fun updateLoading(loading: Boolean, playing: Boolean) {
        if (this.loading != loading || this.playing != playing) {
            this.loading = loading
            this.playing = playing
            requireBinding().apply {
                val playVisible = playing && !loading
                progressBar.visibleOrGone(loading, 1, invisible = true)
                playingView.visibleOrGone(playVisible, 1, invisible = true)
            }
        }
    }

    private var loadedProgress = 0

    private var loading = false
    private var playing = false

    private var registered = false

    private fun registerPlayerView() {
        if (book.musicUrl.isNotEmpty()) {
            loaded
            val player = mainActivity().musicPlayer

            val current = player.currentPlayingUrl == book.musicUrl
            val currentPlaying = current && player.playing()

            updatePlayView(currentPlaying)
            requireBinding().playingView.visibleOrGone(currentPlaying)

            var progress = player.getBookProgress(book)
            val time = player.getTimeText(book)
            requireBinding().timeView.text = time
            if (progress == 100) {
                progress = 0
            }
            requireBinding().seekBar.progress = progress
            val cache = DownloadController.getDownload(book.musicUrl)
            if (cache != null) {
                loadedProgress = cache.percentDownloaded.toInt()
            }

            if (current) {
                player.showMusicView(false)
            }

            if (current) {
                if (registered) return
                registered = true

                requireBinding().seekBar.setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {

                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        player.pause()
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        player.seekToProgress(seekBar!!.progress)
                        player.play()
                    }

                })

                if (currentPlaying) {
                    player.setCallback(object : MusicPlayCallback {
                        override fun onUpdate(value: Float, currentPos: String, duration: String) {
                            requireBinding().seekBar.progress = value.toInt()
                            requireBinding().timeView.text = "$currentPos / $duration"
                        }
                    })
                }

                player.callback = object : MusicInterface {
                    override fun setMusic(url: String) {

                    }

                    override fun setCallback(playCallback: MusicPlayCallback) {

                    }

                    override fun play() {
                        updatePlayView(true)
                    }

                    override fun pause() {
                        updatePlayView(false)
                        updateLoading(loading = false, false)
                    }

                    override fun stop() {
                        updatePlayView(false)
                        updateLoading(loading = false, false)
                    }

                    override fun loading(b: Boolean) {
                        if (player.currentPlayingUrl == book.musicUrl) {
                            updateLoading(b, player.playing())
                        }
                    }
                }
            }
        }
    }

    private fun resume() {
        registerPlayerView()
        updateDownloadView(true)
        if (userLogged()) {
            checkLiked()
        } else {
            updateLikeView()
        }
    }

    override fun onResume() {
        super.onResume()
        resume()
    }

    private fun pause() {
        cancelRunOnUiThread(progressUpdateRun)
        PdfController.getInstance().cancelDownload(book.pdfUrl)
        downloadListener = null
        DownloadController.removeListener(book.musicUrl)
        registered = false
        downloadStopped = false
        loaded = false
        loading = false
        downLoadLoading = false

        player().apply {
            if (bookId == currentBook.id) {
                showMusicView(true)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        pause()
    }

    private var downloading = false
    private var checkDrawable: Drawable? = null

    private var progressUpdateRun: Runnable? = null

    private val downloadCallback = object : DownloadController.DownloadProgressCallback {
        override fun onProgressChanges(manager: DownloadManager, download: Download) {
            //Get download again ->
            if (download.state == Download.STATE_DOWNLOADING) {
                val download = DownloadController.getDownload(download.request.id)!!
                var percent = download.percentDownloaded.toInt()
                requireBinding().downloadProgressView.progress = percent
                if (percent < 0) {
                    percent = 0
                }
                requireBinding().sizeView.text = "$percent%"

                progressUpdateRun?.let {
                    cancelRunOnUiThread(it)
                    progressUpdateRun = null
                }

                if (percent == 100) {
                    downloading = false
                    updateDownloadView(false)
                } else {
                    progressUpdateRun = Runnable {
                        onProgressChanges(manager, download)
                    }
                    runOnUiThread(progressUpdateRun!!, 100)
                }
            } else {
                if (download.state == Download.STATE_FAILED) {
                    showSnackBar("Internetga ulaning!")
                }
            }
        }
    }

    private var downloadStopped = false
    private var loaded = false
    private var downLoadLoading = false

    private fun updateDownloadView(first: Boolean) {
        requireBinding().apply {
            val url = book.musicUrl
            if (url.isEmpty()) return
            val download = DownloadController.getDownload(url)

            if (download == null && !downloading || downloadStopped) {
                progressUpdateRun?.let { cancelRunOnUiThread(it) }
                downloadView.visibleOrGone(true, 1)
                downloadContainer.visibleOrGone(false, 1)
            } else {
                var loading = DownloadController.isDownloading(url)

                downLoadLoading = loading
                if (!first) {
                    loading = downloading
                } else {
                    downloading = loading
                }

                loaded =
                    DownloadController.checkOffline(book.id) && download?.percentDownloaded == 100f

                if (loaded) {
                    downloadContainer.visibleOrGone(false, 1)
                    downloadView.apply {
                        visibleOrGone(true, 1)
                        text = "Offline"
                        if (checkDrawable == null) {
                            checkDrawable = getDrawable(R.drawable.ic_check)
                        }
                        TextViewCompat.setCompoundDrawableTintList(
                            this,
                            ColorStateList.valueOf(context.getColor(R.color.green))
                        )
                        setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            checkDrawable,
                            null,
                            null
                        )
                    }
                } else {
                    downloadView.visibleOrGone(false, 1)
                    downloadContainer.visibleOrGone(true, 1)

                    val p = download?.percentDownloaded?.toInt() ?: 0
                    downloadProgressView.progress = p
                    sizeView.text = "$p%"

                    if (loading) {
                        DownloadController.setListener(url, downloadCallback)
                        if (download != null) {
                            progressUpdateRun = Runnable {
                                downloadCallback.onProgressChanges(
                                    DownloadController.downloadManager,
                                    download
                                )
                            }
                            runOnUiThread(progressUpdateRun!!)
                        }
                        downloadingTextView.text = "Bekor qilish"
                    } else {
                        downloadingTextView.text = "Yuklash"
                    }
                }
            }
            requireBinding().offlineSign.visibleOrGone(loaded, 1)
        }
    }

    private fun download(): Boolean {
        val dwn = DownloadController.download(book.musicUrl)
        if (dwn) {
            showSnackBar("Ko'chirlyapti")
        } else {
            showSnackBar("Boshqa kitob ko'chyapti !")
        }
        return dwn
    }

    private fun togglePlay() {
        requireBinding().apply {
            player().setBook(book)
            if (player().playing()) {
                player().pause()
            } else {
                if (player().currentPlayingUrl != bookId) {
                    player().showMusicView(false)
                }
                player().play()
            }
        }
    }

    private var downloadListener: DownloadListener? = null

    private fun openPdf(url: String, bet: Int = 0) {
        val uri = PdfController.getInstance().getPdf(url)
        if (uri != null) {
            val openUri = File(uri)
            if (openUri.length() > 10) {
                openFragment(PdfFragment(openUri.toUri(), book, bet))
            } else {
                PdfController.getInstance().deletePdf(url)
            }
        }
    }

    fun onDownloaded(page: Int = 0) {
        requireBinding().apply {
            pdfProgressBar.visibleOrGone(false, 1)
            openPdf(book.pdfUrl, page)
        }
    }

    private val loadRelated = true

    private var mundarijalar = arrayListOf<BooksController.Mundarija>()
    private var mundarijaAdapter: MundarijaAdapter? = null
    private var mundarijaLoading = false
    fun updateMundarija() {
        requireBinding().apply {
            if (mundarijalar.isNotEmpty()) {
//                mundarijaTitle.visibleOrGone(true)
//                mundarijaRecyclerView.visibleOrGone(true)
//                mundarijaAdapter?.updateList(mundarijalar)
            }
        }
    }

    private fun loadMundarija() {
        mundarijaLoading = true
        booksController().loadMundarijalar(book.id, object :
            Result<ArrayList<BooksController.Mundarija>>() {
            override fun onResult(result: ArrayList<BooksController.Mundarija>) {
                mundarijalar = result
                mundarijaLoading = false
                updateMundarija()

            }
        })
    }

    private fun openFromMundarija(mundarija: BooksController.Mundarija) {
        if (book.musicOrPdf()) {
            val millis = mundarija.betOrMinute * 60 * 1000
            player().apply {
                seekToMillis(millis.toLong())
                if (!playing()) {
                    play()
                }
            }
        } else {
            handleReadButton(mundarija.betOrMinute - 1)
        }
    }

    private fun handleReadButton(page: Int) {
        requireBinding().apply {
            if (pdfProgressBar.isVisible) return
//            if (userLogged()) {
////                if (booksController().myBooks.find { it.id == book.id } == null) {
////                //    booksController.addBookToLibrary(book) {}
////                }
//            }
            //Check is downloaded
            val controller = PdfController.getInstance()
            val uri = controller.getPdf(book.pdfUrl)
            if (uri == null) {
                downloadListener = object : DownloadListener {
                    override fun onDownloadFailed(e: DownloadException?) {
                        onDownloaded()
                    }

                    override fun onDownloadSuccess() {
                        onDownloaded(page)
                    }

                    override fun onPaused() {

                    }

                    override fun onRemoved() {
                        onDownloaded()
                    }

                    override fun onStart() {

                    }

                    override fun onWaited() {

                    }

                    override fun onDownloading(progress: Long, size: Long) {
                        var p = controller.getProgress(book.pdfUrl)
                        if (p < 10) {
                            p = 10
                        }
                        pdfProgressBar.setProgress(p, true)
                    }
                }
                pdfProgressBar.visibleOrGone(true, 1)
                controller.downloadPdf(book.pdfUrl, downloadListener!!)
            } else {
                onDownloaded(page)
            }
        }
    }

    private var youTubeImageView: YouTubeLayoutBinding? = null

    private fun updateAddBookButton() {
        val has = booksController().myBooks.find { it.id == book.id } != null
        requireBinding().addBookBotton.apply {
            text = if (has) "Kutubxonadan o'chirish" else "Kutubxonaga qo'shish"
            setCompoundDrawablesWithIntrinsicBounds(
                0,
                if (has) R.drawable.ic_delete else R.drawable.add_book_ic,
                0,
                0
            )
        }
    }

    private fun updateTypeView() {
        requireBinding().apply {
            typeView.apply {
                val music = book.musicOrPdf()
                text = if (music) "Audio kitob" else "Pdf"
                setCompoundDrawablesWithIntrinsicBounds(
                    if (music) R.drawable.ic_listen else R.drawable.ic_pdf,
                    0,
                    0,
                    0,
                )
            }
        }
    }

    private fun loadComment() {
        updateCommentView()
        val comments = booksController.getCommentsForBook(book.id)
        if (comments.isNullOrEmpty()) {
            booksController.loadComments(
                book.id,
                1,
                object : Result<ArrayList<BooksController.Comment>>() {
                    override fun onResult(result: ArrayList<BooksController.Comment>) {
                        updateCommentView()
                    }
                })
        }
    }

    fun updateCommentView() {
        requireBinding().apply {
            val comments = booksController.getCommentsForBook(book.id)
            val hasComment = !comments.isNullOrEmpty()
            commentView.apply {
                root.visibleOrGone(hasComment)
                if (hasComment) {
                    val comment = comments!!.first()
                    imageView.circleCrop = true
                    imageView.setData(comment.userPhoto)
                    nameView.text = comment.userName
                    textView16.text = comment.comment
                }
            }
        }
    }

    class RateSheet(private val rate: Float, val bookId: String, val added: () -> Unit) :
        BottomSheetBase<RateSheetBinding>(R.layout.rate_sheet) {
        override fun onViewCreated(binding: RateSheetBinding) {
            super.onViewCreated(binding)
            binding.apply {
                container().titleView.text = "Kitobni baxolang"
                rating.rating = rate
                doneButton.setOnClickListener {
                    val text = editText.editText!!.text.toString()
                    val rating = rating.rating
                    if (text.isEmpty()) {
                        shakeView(editText)
                        return@setOnClickListener
                    }
                    val comment = BooksController.Comment()
                    comment.comment = text
                    comment.rating = rating
                    comment.userId = currentUserId()
                    comment.userName = currentUser().name
                    comment.userPhoto = currentUser().photo
                    comment.bookId = bookId
                    booksController().addComment(comment) {
                        mainActivity()?.showSnackBar("Komment yuborildi")
                    }
                    added.invoke()
                    closeSheet()
                }
            }
        }

        override fun onPause() {
            super.onPause()
            hideKeyboard(mainActivity()!!)
        }

        override fun onResume() {
            super.onResume()
            showKeyboard(rBinding().editText.editText)
        }
    }

    private var costsAdapter: SimpleAdapter<BookTypeChooseBinding, Cost>? = null

    private fun onBookLoaded() {
        requireBinding().apply {
            if (book.musicUrl.isEmpty()) {
                downloadView.visibleOrGone(false)
                downloadContainer.visibleOrGone(false)
                timeView.visibleOrGone(false)
            }
            partView.apply {
                if (book.parentBook.isNotEmpty()) {
                    visibleOrGone(true, 1)
                    text = "${book.part}-qism"
                }
            }
            loadComment()
            commentsButton.setOnClickListener {
                openFragment(CommentsFragment(book))
            }
            commentView.root.setOnClickListener {
                openFragment(CommentsFragment(book))
            }
            sellerLay.callView.apply {
                visibleOrGone(true)
                setOnClickListener {
                    mainActivity().callTo(book.author.phone)
                }
            }
            updateTypeView()
            //-> Type
            if (book.cost.price != 0L) {
                costsAdapter =
                    object : SimpleAdapter<BookTypeChooseBinding, Cost>(R.layout.book_type_choose) {
                        override fun bind(
                            holder: BaseViewHolder<BookTypeChooseBinding>,
                            position: Int,
                            model: Cost
                        ) {
                            holder.binding.apply {
                                nameView.text = getBookTypeText(model.bookType)
                                costView.text = formatCurrency(model.price)
                            }
                        }
                    }.apply {
                        clickListener = object : RecyclerItemClickListener {
                            override fun onClick(position: Int, viewType: Int) {
                                val item = currentList[position]
                            }
                        }
                    }
                typesRecyclerView.apply {
                    adapter = costsAdapter
                    layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

                    val types = booksController.getBookCost(book.id)
                    if (types.isEmpty()) {
                        booksController.getCostsForBook(book.id) {
                            costsAdapter?.setDataList(it)
                        }
                    } else {
                        costsAdapter?.setDataList(types)
                    }
                }
            } else {
            }
//            mundarijaRecyclerView.apply {
//                adapter = MundarijaAdapter(!book.musicOrPdf()).apply {
//                    mundarijaAdapter = this
//                    clickListener = {
//                        val item = mundarijalar[it.layoutPosition]
//                        openFromMundarija(item)
//                    }
//                }
//                layoutManager = LinearLayoutManager(requireContext())
//                booksController().mundarijalar[book.id]?.let {
//                    mundarijalar = it
//                }
//            }
            if (mundarijalar.isNotEmpty()) {
                updateMundarija()
            } else {
                loadMundarija()
            }
            val r = book.rating.toFloat()
            ratingTextView.text = r.toString()
            rating.rating = r
            mundarijaButton.setOnClickListener {
                openFragment(MundarijaFragment(book))
            }
            aboutMore.setOnClickListener {
                showDescriptionForBook(mainActivity(), book)
            }
            addLauncherButton.setOnClickListener {
                createShortcutOnHome(requireActivity(), book)
            }
            pdfProgressBar.visibleOrGone(false)
            readButton.visibleOrGone(book.pdfUrl.isNotEmpty())
            readButton.setOnClickListener {
                handleReadButton(0)
            }
            updateAddBookButton()
            addBookBotton.setOnClickListener {
                val has = booksController().myBooks.find { it.id == book.id } != null
                if (has) {
                    booksController().deleteBookFromLibrary(book.id) {
                        updateAddBookButton()
                        showSnackBar("Kitob kutubxonaga o'chirildi")
                    }
                } else {
                    booksController.addBookToLibrary(book) {
                        updateAddBookButton()
                        showSnackBar("Kitob kutubxonaga qo'shildi")
                    }
                }
            }
            shareView.setOnClickListener {
                shareBook(it, book)
            }
            photoView.cornerRadius = dp(8f).toFloat()

            if (book.youTubeUrl.isNotEmpty()) {
                if (youTubeImageView == null) {
                    youTubeImageView =
                        inflateBinding<YouTubeLayoutBinding>(null, R.layout.you_tube_layout).also {
                            containerL.addView(it.root)
                        }
                }
                youTubeImageView!!.apply {
                    val media =
                        Media(book.youTubeUrl, Media.TYPE_VIDEO)
                    playerView.scaleType = ImageView.ScaleType.CENTER_CROP
                    playerView.loadImage(media)
                }
            }
            rating.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
                requireLogin {
                    openSheet(RateSheet(rating, book.id, { updateCommentView() }))
                }
            }
            fikrQoldirish.setOnClickListener {
                requireLogin {
                    openSheet(RateSheet(rating.rating, book.id, { updateCommentView() }))
                }
            }
            playingView.setAnimationFromUrl(playingLottie)
            playingView.repeatMode = LottieDrawable.RESTART
            playingView.repeatCount = LottieDrawable.INFINITE
            playingView.playAnimation()

            downloadContainer.setOnClickListener {
                if (!downloading) {
                    if (download()) {
                        downloadStopped = false
                        downloading = true
                    }
                } else {
                    //Pause
                    downloadStopped = true
                    downloading = false
                    DownloadController.stopDownload(book.musicUrl)
                    showSnackBar("To'xtatildi")
                }
                updateDownloadView(false)
            }
            downloadView.setOnClickListener {
                if (loaded) {
                    showSnackBar("Offline eshiting!")
                    return@setOnClickListener
                }
                if (download()) {
                    downloadStopped = false
                    downloading = true
                }
                updateDownloadView(false)
            }

            heartView.setOnClickListener {
                doLike()
            }
            book.apply {
                photoView.requestListener = object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
//                        resource?.let {
//                            palette = Palette.from(it).generate()
//                            updateColors()
//                        }
                        return false
                    }
                }
                photoView.setData(photo)
                textView12.text = name
                catView.text = category.name + " - ${book.authorName}"
                infoView.text = getInfo(false)
                aboutMore.text = about

                sellerLay.apply {
                    root.setOnClickListener {
                        openFragment(AuthorBooksFragment(book.author))
                    }
                    imageView.scaleX = 0.8f
                    imageView.scaleY = 0.8f
                    imageView.circleCrop = true
                    imageView.setData(book.author.photo)
                    nameView.text = book.author.name
                    textView16.text = "Sotuvchi"
                }

                playButton.apply {
                    if (musicUrl.isEmpty()) {
                        visibleOrGone(false)
                        seekBar.visibleOrGone(false)
                    } else {
                        setOnClickListener {
                            togglePlay()
                        }
                    }
                }

                partsRecyclerView.apply {
                    layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    adapter = object : BooksFragment.InnerAdapter(false) {

                        override fun bind(holder: BaseViewHolder<ViewDataBinding>, model: Book) {
                            super.bind(holder, model)
                            holder.apply {
                                binding.apply {
                                    if (this is BookItemMediumBinding) {
                                        partView.apply {
                                            text = "${model.part}-qism"
                                            visibleOrGone(true)
                                        }
                                        root.apply {
                                            if (model.id == book.id) {
                                                setBackgroundColor(getThemeColor(R.attr.colorBackground))
                                            } else {
                                                background = null
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }.apply {
                        clickListener = {
                            it.apply {
                                val bookId = bookParts.getOrNull(layoutPosition)?.id
                                if (bookId != null && bookId != book.id) {
                                    openFragment(DetailsFragment(bookId))
                                }
                            }
                        }
                        bookPartsAdapter = this
                    }
                }
                if (book.parentBook.isNotEmpty()) {
                    bookParts = ArrayList(booksController().getBookParts(book.parentBook))
                    if (bookParts.isEmpty() || bookParts.size == 1 && bookParts.first().id == book.id) {
                        loadBookParts()
                    } else {
                        updateBookParts(false)
                    }
                }

                if (loadRelated) {
                    recyclerView.apply {
                        moreTitle.visibleOrGone(true)
                        visibleOrGone(true)
                        adapter = BooksFragment.InnerAdapter().apply {
                            clickListener = {
                                it.apply {
                                    val bookId = moreList[layoutPosition].id
                                    openFragment(DetailsFragment(bookId))
                                }
                            }
                            mAdapter = this
                            val list = booksController().relatedBooks[bookId]
                            val after = booksController().getBooksForCategory(book.category.id)
                            val afterId = after.lastOrNull()?.category?.id ?: ""
                            if (list.isNullOrEmpty()) {
                                loading = true

                                booksController.loadBooks(
                                    categoryId = book.category.id,
                                    relatedBookId = book.id,
                                    startAfter = afterId,
                                    result = object : Result<ArrayList<Book>>() {
                                        override fun onResult(result: ArrayList<Book>) {
                                            loading = false
                                            moreList = result
                                            if (result.isEmpty()) {
                                                if (after.isNotEmpty()) {
                                                    moreList = (after as ArrayList<Book>).apply {
                                                        removeIf { it.id == book.id }
                                                    }
                                                }
                                            }
                                            updateList(moreList)
                                            if (moreList.isEmpty()) {
                                                recyclerView.visibleOrGone(false)
                                                emptyMore.visibleOrGone(true)
                                            }
                                        }
                                    }
                                )
                            } else {
                                moreList = list
                                updateList(moreList)
                                if (moreList.isEmpty()) {
                                    emptyMore.visibleOrGone(true)
                                }

                            }
                        }
                        layoutManager =
                            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    }
                }
                booksController().increaseBookView(bookId)
            }
            loadRelated
            if (book.musicUrl.isNotEmpty()) {
                if (play) {
                    if (player().currentPlayingUrl == book.musicUrl && player().playing()) return
                    togglePlay()
                }
            }
        }
    }

    override fun onViewCreated(binding: DetailsFragmentBinding) {
        likeActive = Color.RED
        likeNotActive = Color.LTGRAY

        //  updatePlayView()
        requireActionBar().apply {
            title = ""
            lightStatusBar = true
            backgroundColor = requireContext().getThemeColor(R.attr.colorSurface)
            iconsColor = requireContext().getThemeColor(R.attr.colorOnSurfaceMedium)
            addMenu(R.drawable.ic_search).apply {
                setOnClickListener {
                    openFragment(SearchFragment())
                }
            }
            addMenu(R.drawable.more_icon).setOnClickListener {
                showPopup(it) {
                    addItem(0, "Share", R.drawable.ic_reshare) {
                        shareBook(it, book)
                    }
                    val downloaded = PdfController.getInstance().getPdf(book.pdfUrl) != null
                    if (downloaded) {
                        addItem(1, "Xotiradan o'chirish", R.drawable.ic_delete) {
                            PdfController.getInstance().deletePdf(book.pdfUrl)
                        }
                    }
                    if (currentUserId() == book.publisherId) {
                        addItem(2, "Qism qo'shish", R.drawable.ic_add_library) {
                            openFragment(AddBookFragment().apply {
                                this.book = this@DetailsFragment.book
                            })
                        }
                        addItem(3, "Kitobni o'chirish", R.drawable.ic_delete) {
                            booksController().deleteBook(book.id) {
                                closeLastFragment()
                            }
                        }
                        addItem(3, "Mundarija", R.drawable.ic_add_box) {
                            openFragment(MundarijaFragment(book))
                        }
                    }
                }
            }
        }

        if (bookId.isNotEmpty()) {
            val fromCache = booksController.getBook(bookId)
            if (fromCache != null) {
                book = fromCache
                onBookLoaded()
            } else {
                showProgressBar(true)
                booksController().loadBook(bookId) {
                    showProgressBar(false)
                    if (it != null) {
                        book = it
                        onBookLoaded()
                    } else {
                        toast("Kitob topilmadi!")
                    }
                }
            }
        }

    }

    private fun showProgressBar(show: Boolean) {
        requireBinding().apply {
            containerView.apply {
//                children.forEach {
//                    if (it != progessBar) {
//                        visibleOrGone(!show)
//                    }
//                }
                progessBar.visibleOrGone(show, 1)
            }
        }
    }
}

fun Context.callTo(phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL)
        val ph = phone.ifEmpty { "+998971871415" }
        intent.data = Uri.parse("tel:$ph")
        startActivity(intent)
    }catch (e: Exception) {
        //
    }
}