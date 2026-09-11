package io.github.yuroyami.libmpvkt

/** Turns the observed properties into one [MpvPlaybackState]. Pure, so the status table is a unit test. */
internal object PlaybackStateReducer {
    data class Inputs(
        val idleActive: Boolean, val coreIdle: Boolean, val pause: Boolean, val pausedForCache: Boolean, val eofReached: Boolean,
        val seeking: Boolean, val timePos: Double?, val duration: Double?, val path: String?, val cachePercent: Int?,
        val seekable: Boolean, val speed: Double,
    )

    fun reduce(i: Inputs): MpvPlaybackState {
        val status = when {
            i.idleActive || i.path == null -> MpvPlaybackState.Status.Idle
            // With keep-open-pause=no mpv does not pause at the end, so eof-reached alone decides.
            i.eofReached -> MpvPlaybackState.Status.Ended
            i.pausedForCache -> MpvPlaybackState.Status.Buffering
            i.pause -> MpvPlaybackState.Status.Paused
            i.coreIdle -> MpvPlaybackState.Status.Loading
            else -> MpvPlaybackState.Status.Playing
        }
        return MpvPlaybackState(status, i.path, i.timePos, i.duration, i.seeking, i.cachePercent, i.seekable, i.speed)
    }
}
