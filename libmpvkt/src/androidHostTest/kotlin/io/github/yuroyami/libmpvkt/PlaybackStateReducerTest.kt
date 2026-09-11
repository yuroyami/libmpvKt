package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackStateReducerTest {
    private val base = PlaybackStateReducer.Inputs(idleActive = false, coreIdle = false, pause = false, pausedForCache = false, eofReached = false, seeking = false, timePos = 12.0, duration = 60.0, path = "a.mkv", cachePercent = 100, seekable = true, speed = 1.0)

    @Test
    fun theStatusTable() {
        assertEquals(MpvPlaybackState.Status.Playing, PlaybackStateReducer.reduce(base).status)
        assertEquals(MpvPlaybackState.Status.Paused, PlaybackStateReducer.reduce(base.copy(pause = true)).status)
        assertEquals(MpvPlaybackState.Status.Buffering, PlaybackStateReducer.reduce(base.copy(pausedForCache = true)).status)
        assertEquals(MpvPlaybackState.Status.Ended, PlaybackStateReducer.reduce(base.copy(eofReached = true, pause = true)).status)
        // keep-open-pause=no: mpv holds the last frame without pausing, and the core goes idle.
        assertEquals(MpvPlaybackState.Status.Ended, PlaybackStateReducer.reduce(base.copy(eofReached = true, coreIdle = true)).status)
        assertEquals(MpvPlaybackState.Status.Loading, PlaybackStateReducer.reduce(base.copy(coreIdle = true, timePos = null, duration = null)).status)
        assertEquals(MpvPlaybackState.Status.Idle, PlaybackStateReducer.reduce(base.copy(idleActive = true, path = null)).status)
    }

    @Test
    fun theValuesPassThrough() {
        val s = PlaybackStateReducer.reduce(base.copy(seeking = true, cachePercent = 42, speed = 1.5))
        assertEquals(12.0, s.positionSeconds)
        assertEquals(60.0, s.durationSeconds)
        assertEquals(true, s.seeking)
        assertEquals(42, s.bufferingPercent)
        assertEquals(1.5, s.speed)
        assertEquals("a.mkv", s.path)
    }
}
