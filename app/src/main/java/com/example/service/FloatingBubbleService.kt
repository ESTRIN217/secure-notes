package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainActivity
import com.example.R
import com.example.data.SharedPreferencesRepository
import com.example.data.local.NoteDatabase
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.Note
import com.example.data.model.TextSegment
import com.example.ui.floating.FloatingBubbleContent
import com.example.ui.floating.FloatingLifecycleOwner
import com.example.ui.floating.FloatingNoteCard
import com.example.ui.floating.FloatingQuickNoteViewModel
import com.example.ui.floating.ResizeCorner
import com.example.ui.theme.MyApplicationTheme
import com.example.util.RichTextConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val lifecycleOwner = FloatingLifecycleOwner()

    private var isExpanded by mutableStateOf(false)
    private var bubbleX = 16
    private var bubbleY = 300
    private var cardX: Int? = null
    private var cardY: Int? = null
    private var cardW: Int? = null
    private var cardH: Int? = null

    private val recentNotesState = MutableStateFlow<List<Note>>(emptyList())

    companion object {
        const val ACTION_STOP = "com.example.service.ACTION_STOP_FLOATING"
        private const val CHANNEL_ID = "floating_bubble_channel"
        private const val NOTIFICATION_ID = 1004

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundNotification()
        observeNotes()
        setupOverlayView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()
        removeOverlayView()
        lifecycleOwner.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.floating_mode_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.floating_mode_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingBubbleService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.floating_mode_notification_title))
            .setContentText(getString(R.string.floating_mode_notification_text))
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.floating_mode_stop), stopIntent)
            .addAction(0, getString(R.string.floating_mode_open_app), openIntent)
            .setOngoing(true)
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
    }

    private fun observeNotes() {
        serviceScope.launch(Dispatchers.IO) {
            val db = NoteDatabase.getDatabase(applicationContext)
            db.noteDao.getAllNotesFlow().collectLatest { notes ->
                recentNotesState.value = notes.filter { !it.isDeleted }
            }
        }
    }

    private fun setupOverlayView() {
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner.attachToView(this)
            setContent {
                val repo = remember { SharedPreferencesRepository(applicationContext) }
                val isDark = isDarkThemeEnabled(repo)
                val isDynamic = repo.getIsDynamicColor()
                val recentNotes by recentNotesState.collectAsState()
                val quickNoteViewModel: FloatingQuickNoteViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return FloatingQuickNoteViewModel() as T
                        }
                    }
                )
                val quickTitle by quickNoteViewModel.title.collectAsState()
                val quickSegments by quickNoteViewModel.segments.collectAsState()
                val quickSelection by quickNoteViewModel.selection.collectAsState()
                val quickPendingStyle by quickNoteViewModel.pendingTypingStyle.collectAsState()
                val quickActiveStyles by quickNoteViewModel.activeTextStyles.collectAsState()

                MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamic) {
                    if (!isExpanded) {
                        FloatingBubbleContent(
                            onClick = { expandOverlay() },
                            onDrag = { dx, dy -> moveOverlayBy(dx, dy) },
                            onDragEnd = { snapBubbleToEdge() }
                        )
                    } else {
                        FloatingNoteCard(
                            recentNotes = recentNotes,
                            onHeaderDrag = { dragAmount -> moveOverlayBy(dragAmount.x, dragAmount.y) },
                            title = quickTitle,
                            segments = quickSegments,
                            selection = quickSelection,
                            pendingTypingStyle = quickPendingStyle,
                            activeTextStyles = quickActiveStyles,
                            onTitleChange = quickNoteViewModel::onTitleChange,
                            onSegmentsChange = quickNoteViewModel::onSegmentsChange,
                            onSelectionChange = quickNoteViewModel::onSelectionChange,
                            onToggleTag = quickNoteViewModel::toggleTag,
                            onSaveNote = { title, segments ->
                                saveQuickNote(title, segments)
                                quickNoteViewModel.clear()
                            },
                            onClear = quickNoteViewModel::clear,
                            onResizeCorner = { corner, dragAmount ->
                                resizeOverlayBy(corner, dragAmount.x, dragAmount.y)
                            },
                            onResetLayout = { resetCardLayout() },
                            onOpenApp = { noteId -> openMainActivity(noteId) },
                            onMinimize = { collapseOverlay() },
                            onClose = { stopSelf() }
                        )
                    }
                }
            }
        }
        composeView = view
        val params = buildBubbleParams()
        setupTouchListener(view)
        windowManager.addView(view, params)
    }

    private fun isDarkThemeEnabled(repo: SharedPreferencesRepository): Boolean {
        val option = repo.getDarkModeOption()
        return when (option) {
            com.example.DarkModeOption.ON -> true
            com.example.DarkModeOption.OFF -> false
            com.example.DarkModeOption.SYSTEM -> {
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun buildBubbleParams(): WindowManager.LayoutParams {
        val sizePx = (56 * resources.displayMetrics.density).toInt()
        return WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bubbleX
            y = bubbleY
        }
    }

    private fun cardSizeLimits(): CardSizeLimits {
        val metrics = getDisplayMetrics()
        return CardSizeLimits(
            minW = (280 * metrics.density).toInt(),
            minH = (360 * metrics.density).toInt(),
            maxW = (metrics.widthPixels * 0.92f).toInt(),
            maxH = (metrics.heightPixels * 0.75f).toInt()
        )
    }

    private fun buildCardParams(): WindowManager.LayoutParams {
        val metrics = getDisplayMetrics()
        val limits = cardSizeLimits()
        val widthPx = cardW ?: (340 * metrics.density).toInt().coerceAtMost(limits.maxW)
        val heightPx = cardH ?: (460 * metrics.density).toInt().coerceAtMost(limits.maxH)
        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cardX ?: ((metrics.widthPixels - widthPx) / 2)
            y = cardY ?: ((metrics.heightPixels - heightPx) / 2)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        return resources.displayMetrics
    }

    private fun setupTouchListener(view: View) {
        view.setOnTouchListener { _, event ->
            if (isExpanded && event.action == MotionEvent.ACTION_OUTSIDE) {
                collapseOverlay()
                return@setOnTouchListener true
            }
            false
        }
    }

    private fun currentOverlayParams(): WindowManager.LayoutParams? =
        composeView?.layoutParams as? WindowManager.LayoutParams

    private fun moveOverlayBy(dxPx: Float, dyPx: Float) {
        val view = composeView ?: return
        val params = currentOverlayParams() ?: return
        val metrics = getDisplayMetrics()
        val (clampedX, clampedY) = clampOverlayPosition(
            x = params.x + dxPx.toInt(),
            y = params.y + dyPx.toInt(),
            winWidth = params.width,
            winHeight = params.height,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels
        )
        params.x = clampedX
        params.y = clampedY
        if (isExpanded) {
            cardX = clampedX
            cardY = clampedY
        } else {
            bubbleX = clampedX
            bubbleY = clampedY
        }
        windowManager.updateViewLayout(view, params)
    }

    private fun snapBubbleToEdge() {
        val view = composeView ?: return
        val params = currentOverlayParams() ?: return
        if (isExpanded) return
        val screenWidth = getDisplayMetrics().widthPixels
        val margin = (16 * resources.displayMetrics.density).toInt()
        params.x = if (params.x < screenWidth / 2) margin else (screenWidth - params.width - margin)
        bubbleX = params.x
        bubbleY = params.y
        windowManager.updateViewLayout(view, params)
    }

    private fun resizeOverlayBy(corner: ResizeCorner, dxPx: Float, dyPx: Float) {
        val view = composeView ?: return
        if (!isExpanded) return
        val params = currentOverlayParams() ?: return
        val metrics = getDisplayMetrics()
        val limits = cardSizeLimits()
        val result = applyCornerResize(
            corner = corner,
            paramsX = params.x,
            paramsY = params.y,
            paramsWidth = params.width,
            paramsHeight = params.height,
            dxPx = dxPx.toInt(),
            dyPx = dyPx.toInt(),
            minWidth = limits.minW,
            minHeight = limits.minH,
            maxWidth = limits.maxW,
            maxHeight = limits.maxH,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels
        )
        params.x = result.x
        params.y = result.y
        params.width = result.width
        params.height = result.height
        cardX = result.x
        cardY = result.y
        cardW = result.width
        cardH = result.height
        windowManager.updateViewLayout(view, params)
    }

    private fun resetCardLayout() {
        val view = composeView ?: return
        cardX = null
        cardY = null
        cardW = null
        cardH = null
        windowManager.updateViewLayout(view, buildCardParams())
    }

    private fun sanitizeCardLayout() {
        val metrics = getDisplayMetrics()
        val limits = cardSizeLimits()
        cardW = cardW?.coerceIn(limits.minW, limits.maxW)
        cardH = cardH?.coerceIn(limits.minH, limits.maxH)
        val params = buildCardParams()
        val (clampedX, clampedY) = clampOverlayPosition(
            params.x, params.y, params.width, params.height,
            metrics.widthPixels, metrics.heightPixels
        )
        cardX = clampedX
        cardY = clampedY
    }

    private fun expandOverlay() {
        val view = composeView ?: return
        isExpanded = true
        sanitizeCardLayout()
        val params = buildCardParams()
        windowManager.updateViewLayout(view, params)
    }

    private fun collapseOverlay() {
        val view = composeView ?: return
        isExpanded = false
        val params = buildBubbleParams()
        windowManager.updateViewLayout(view, params)
    }

    private fun saveQuickNote(title: String, segments: List<TextSegment>) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = NoteDatabase.getDatabase(applicationContext)
                val nonEmpty = segments.filter { it.text.isNotEmpty() }
                val finalSegments = nonEmpty.ifEmpty { listOf(TextSegment(text = "")) }
                val block = DataBlock(
                    type = BlockType.TEXT,
                    content = RichTextConverter.segmentsToPlainText(finalSegments),
                    richTextJson = TextSegment.serialize(finalSegments)
                )
                val jsonContent = DataBlock.serialize(listOf(block))
                val note = Note(
                    title = title.ifBlank { getString(R.string.btn_new_note) },
                    content = jsonContent,
                    isEncrypted = false,
                    lastModified = System.currentTimeMillis()
                )
                db.noteDao.insertNote(note)
            } catch (e: Exception) {
                Log.e("FloatingBubbleService", "Error saving quick note", e)
            }
        }
    }

    private fun openMainActivity(noteId: Int?) {
        collapseOverlay()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            noteId?.let { putExtra("open_note_id", it) }
        }
        startActivity(intent)
    }

    private fun removeOverlayView() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e("FloatingBubbleService", "Error removing view", e)
            }
            composeView = null
        }
    }
}

internal fun clampOverlayPosition(
    x: Int,
    y: Int,
    winWidth: Int,
    winHeight: Int,
    screenWidth: Int,
    screenHeight: Int
): Pair<Int, Int> {
    if (winWidth >= screenWidth) return 0 to y.coerceIn(0, (screenHeight - winHeight).coerceAtLeast(0))
    if (winHeight >= screenHeight) return x.coerceIn(0, screenWidth - winWidth) to 0
    return x.coerceIn(0, screenWidth - winWidth) to y.coerceIn(0, screenHeight - winHeight)
}

internal data class ResizedLayout(val x: Int, val y: Int, val width: Int, val height: Int)

private data class CardSizeLimits(val minW: Int, val minH: Int, val maxW: Int, val maxH: Int)

internal fun applyCornerResize(
    corner: ResizeCorner,
    paramsX: Int,
    paramsY: Int,
    paramsWidth: Int,
    paramsHeight: Int,
    dxPx: Int,
    dyPx: Int,
    minWidth: Int,
    minHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
    screenWidth: Int,
    screenHeight: Int
): ResizedLayout {
    val newWidth = (paramsWidth + dxPx * corner.signX).coerceIn(minWidth, maxWidth)
    val newHeight = (paramsHeight + dyPx * corner.signY).coerceIn(minHeight, maxHeight)
    val movedX = if (corner.movesX) paramsX - (newWidth - paramsWidth) else paramsX
    val movedY = if (corner.movesY) paramsY - (newHeight - paramsHeight) else paramsY
    val (clampedX, clampedY) = clampOverlayPosition(
        movedX, movedY, newWidth, newHeight, screenWidth, screenHeight
    )
    return ResizedLayout(clampedX, clampedY, newWidth, newHeight)
}
