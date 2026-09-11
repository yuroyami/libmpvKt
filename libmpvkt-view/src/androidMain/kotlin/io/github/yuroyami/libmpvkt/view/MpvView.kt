package io.github.yuroyami.libmpvkt.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import io.github.yuroyami.libmpvkt.*
import kotlinx.coroutines.flow.StateFlow
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi

@OptIn(InternalLibmpvKtApi::class)
/**
 * A view that shows mpv's output and owns the surface handshake with the core.
 *
 * [initialize], then [playFile]. The core is [mpv]; the typed API is there for everything this
 * class does not wrap. One core per view; [destroy] closes it. Every member runs on the main
 * thread. The child view is a `SurfaceView` or a `TextureView` by [MpvOptions.surfaceType]; read
 * [SurfaceType] before choosing.
 */
public class MpvView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    public var mpv: Mpv? = null
        private set

    private var options: MpvOptions = MpvOptions()
    private var mpvSurface: Surface? = null
    private var surfaceChild: View? = null
    private var surfaceCallback: SurfaceHolder.Callback? = null

    /**
     * Starts [core] with [options] and installs the surface child. Calling it on a running view
     * closes the old core first and hands the surface it already has to the new one.
     */
    public fun initialize(options: MpvOptions = MpvOptions(), core: Mpv = Mpv.create(context.applicationContext)) {
        mpv?.let { old -> mpvSurface?.let { SurfaceHandshake.detach(old, it) }; old.close() }
        this.options = options.let { if (it.displayFps == null) it.copy(displayFps = displayRefreshRate()) else it }
        this.options.applyTo(core)
        core.initialize().getOrThrow()
        mpv = core
        installSurfaceChild()
    }

    /** Starts [pathOrUrl]. Fine before the surface exists: the core starts with `vo=null`, and the surface brings the picture. */
    public fun playFile(pathOrUrl: String, mode: LoadFileMode = LoadFileMode.Replace): MpvResult<MpvNode> =
        checkNotNull(mpv) { "initialize() first" }.command(MpvCommands.loadFile(pathOrUrl, mode))

    /** Detaches the surface, removes the child and closes the core. Safe to call twice. */
    public fun destroy() {
        (surfaceChild as? TextureView)?.surfaceTextureListener = null
        surfaceCallback?.let { (surfaceChild as? SurfaceView)?.holder?.removeCallback(it) }
        surfaceCallback = null
        mpv?.let { core -> mpvSurface?.let { SurfaceHandshake.detach(core, it) } }
        releaseOwnedSurface()
        removeAllViews()
        surfaceChild = null
        mpv?.close()
        mpv = null
    }

    private fun core(): Mpv = checkNotNull(mpv) { "initialize() first" }

    public var paused: Boolean
        get() = core()[MpvProperties.Pause].getOrNull() ?: false
        set(value) { core()[MpvProperties.Pause] = value }

    /** Seconds, sub-second precise; null before the first frame. Setting it seeks exactly. */
    public var timePos: Double?
        get() = core()[MpvProperties.TimePos].getOrNull()
        set(value) { if (value != null) core()[MpvProperties.TimePos] = value }

    public val duration: Double? get() = core()[MpvProperties.Duration].getOrNull()

    public var speed: Double
        get() = core()[MpvProperties.Speed].getOrNull() ?: 1.0
        set(value) { core()[MpvProperties.Speed] = value }

    public var volume: Double
        get() = core()[MpvProperties.Volume].getOrNull() ?: 100.0
        set(value) { core()[MpvProperties.Volume] = value }

    public var muted: Boolean
        get() = core()[MpvProperties.Mute].getOrNull() ?: false
        set(value) { core()[MpvProperties.Mute] = value }

    public var audioTrack: TrackSelection
        get() = core()[MpvProperties.Aid].getOrNull() ?: TrackSelection.Auto
        set(value) { core()[MpvProperties.Aid] = value }

    public var subtitleTrack: TrackSelection
        get() = core()[MpvProperties.Sid].getOrNull() ?: TrackSelection.Auto
        set(value) { core()[MpvProperties.Sid] = value }

    public val tracks: List<MpvTrack> get() = core()[MpvProperties.TrackList].getOrNull().orEmpty()
    public val chapters: List<MpvChapter> get() = core()[MpvProperties.ChapterList].getOrNull().orEmpty()
    public val hwdecActive: String get() = core()[MpvProperties.HwdecCurrent].getOrNull() ?: "no"
    public val playback: StateFlow<MpvPlaybackState>? get() = mpv?.playback

    public fun seek(seconds: Double, mode: SeekMode = SeekMode.Relative, precision: SeekPrecision = SeekPrecision.Exact): MpvResult<MpvNode> =
        core().command(MpvCommands.seek(seconds, mode, precision))

    public fun screenshot(): MpvResult<Bitmap> = core().screenshot()

    /**
     * The panel's refresh rate, or null when this context has no display. An application context
     * has none, and asking one for a display throws rather than answering.
     */
    private fun displayRefreshRate(): Double? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate?.toDouble()
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate.toDouble()
        }
    }.getOrNull()

    private fun installSurfaceChild() {
        val core = core()
        val existing = surfaceChild
        val wantsTexture = options.surfaceType == SurfaceType.Texture
        val rightKind = (existing is TextureView && wantsTexture) || (existing is SurfaceView && !wantsTexture)
        if (existing != null && rightKind) {
            mpvSurface?.takeIf { it.isValid }?.let { s -> SurfaceHandshake.attach(core, s, options.vo); SurfaceHandshake.resize(core, s, existing.width, existing.height) }
            return
        }
        removeAllViews()
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        surfaceChild = if (wantsTexture) {
            TextureView(context).also { tv ->
                tv.layoutParams = lp
                tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { attach(Surface(st), w, h) }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) { mpv?.let { core -> mpvSurface?.let { SurfaceHandshake.resize(core, it, w, h) } } }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { detach(); return true }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                }
                addView(tv)
            }
        } else {
            SurfaceView(context).also { sv ->
                sv.layoutParams = lp
                val callback = object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) { attach(holder.surface, sv.width, sv.height) }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) { mpv?.let { SurfaceHandshake.resize(it, holder.surface, w, h) } }
                    override fun surfaceDestroyed(holder: SurfaceHolder) { detach() }
                }
                surfaceCallback = callback
                sv.holder.addCallback(callback)
                addView(sv)
            }
        }
    }

    private fun attach(surface: Surface, width: Int, height: Int) {
        val core = mpv ?: return
        mpvSurface = surface
        SurfaceHandshake.attach(core, surface, options.vo)
        if (width > 0 && height > 0) SurfaceHandshake.resize(core, surface, width, height)
    }

    private fun detach() {
        mpv?.let { core -> mpvSurface?.let { SurfaceHandshake.detach(core, it) } }
        releaseOwnedSurface()
    }

    /** A TextureView's Surface is ours to release; a SurfaceView's belongs to its holder. */
    private fun releaseOwnedSurface() {
        if (surfaceChild is TextureView) mpvSurface?.release()
        mpvSurface = null
    }
}
