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
import com.example.MainActivity
import com.example.R
import com.example.data.SharedPreferencesRepository
import com.example.data.local.NoteDatabase
import com.example.data.model.DataBlock
import com.example.data.model.Note
import com.example.ui.floating.FloatingBubbleContent
import com.example.ui.floating.FloatingLifecycleOwner
import com.example.ui.floating.FloatingNoteCard
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val lifecycleOwner = FloatingLifecycleOwner()

    private var isExpanded by mutableStateOf(false)
    private var bubbleX = 16
    private var bubbleY = 300

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
            .setSmallIcon(R.mipmap.ic_launcher)
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

                MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamic) {
                    if (!isExpanded) {
                        FloatingBubbleContent(onClick = { expandOverlay() })
                    } else {
                        FloatingNoteCard(
                            recentNotes = recentNotes,
                            onSaveNote = { title, content -> saveQuickNote(title, content) },
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
        setupTouchListener(view, params)
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

    private fun buildCardParams(): WindowManager.LayoutParams {
        val metrics = getDisplayMetrics()
        val widthPx = (340 * metrics.density).toInt().coerceAtMost((metrics.widthPixels * 0.92f).toInt())
        val heightPx = (460 * metrics.density).toInt().coerceAtMost((metrics.heightPixels * 0.75f).toInt())
        return WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        return resources.displayMetrics
    }

    private fun setupTouchListener(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchDownX = 0f
        var touchDownY = 0f
        var touchDownTime = 0L

        view.setOnTouchListener { _, event ->
            if (isExpanded) {
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    collapseOverlay()
                    return@setOnTouchListener true
                }
                return@setOnTouchListener false
            }
            handleBubbleTouch(event, params, startX, startY, touchDownX, touchDownY, touchDownTime,
                onUpdateDown = { sx, sy, tx, ty, time ->
                    startX = sx; startY = sy; touchDownX = tx; touchDownY = ty; touchDownTime = time
                }
            )
        }
    }

    private fun handleBubbleTouch(
        event: MotionEvent,
        params: WindowManager.LayoutParams,
        startX: Int,
        startY: Int,
        touchDownX: Float,
        touchDownY: Float,
        touchDownTime: Long,
        onUpdateDown: (Int, Int, Float, Float, Long) -> Unit
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onUpdateDown(params.x, params.y, event.rawX, event.rawY, System.currentTimeMillis())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = startX + (event.rawX - touchDownX).toInt()
                params.y = startY + (event.rawY - touchDownY).toInt()
                composeView?.let { windowManager.updateViewLayout(it, params) }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = abs(event.rawX - touchDownX)
                val dy = abs(event.rawY - touchDownY)
                val duration = System.currentTimeMillis() - touchDownTime
                if (dx < 10 && dy < 10 && duration < 250) {
                    expandOverlay()
                } else {
                    snapBubbleToEdge(params)
                }
                return true
            }
        }
        return false
    }

    private fun snapBubbleToEdge(params: WindowManager.LayoutParams) {
        val screenWidth = getDisplayMetrics().widthPixels
        val margin = (16 * resources.displayMetrics.density).toInt()
        params.x = if (params.x < screenWidth / 2) margin else (screenWidth - params.width - margin)
        bubbleX = params.x
        bubbleY = params.y
        composeView?.let { windowManager.updateViewLayout(it, params) }
    }

    private fun expandOverlay() {
        val view = composeView ?: return
        isExpanded = true
        val params = buildCardParams()
        windowManager.updateViewLayout(view, params)
    }

    private fun collapseOverlay() {
        val view = composeView ?: return
        isExpanded = false
        val params = buildBubbleParams()
        windowManager.updateViewLayout(view, params)
    }

    private fun saveQuickNote(title: String, content: String) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = NoteDatabase.getDatabase(applicationContext)
                val blocks = DataBlock.migrateLegacyContent(content)
                val jsonContent = DataBlock.serialize(blocks)
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
