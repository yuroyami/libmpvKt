package io.github.yuroyami.libmpvkt

/**
 * What the player is doing, in one value. [Mpv.playback] keeps it current from the properties
 * mpv observes; the reduction itself is a pure function, so the status table is a unit test.
 */
public data class MpvPlaybackState(
    val status: Status,
    val path: String?,
    val positionSeconds: Double?,
    val durationSeconds: Double?,
    val seeking: Boolean,
    val bufferingPercent: Int?,
    val seekable: Boolean,
    val speed: Double,
) {
    /** The six states a player screen has to tell apart. */
    public enum class Status { Idle, Loading, Playing, Paused, Buffering, Ended }
}
