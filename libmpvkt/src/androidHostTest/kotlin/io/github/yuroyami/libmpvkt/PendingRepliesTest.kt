package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/** The async calls waiting for mpv, and what they get when the core goes away. */
class PendingRepliesTest {

    @Test
    fun aReplyReachesTheCallThatWaitsForIt() = runTest {
        val replies = PendingReplies()
        val first = replies.register(1)!!
        val second = replies.register(2)!!
        replies.complete(2, MpvEvent.Seek)
        assertEquals(MpvEvent.Seek, second.await())
        assertFalse(first.isCompleted)
    }

    @Test
    fun endingAnswersTheWaitingCallsAndRefusesNewOnes() = runTest {
        val replies = PendingReplies()
        val waiting = replies.register(1)!!
        replies.end()
        assertNull(waiting.await(), "a waiting call learns that no reply will come")
        assertNull(replies.register(2), "a call after the end is refused at once")
    }

    @Test
    fun aRemovedCallIsNotAnswered() {
        val replies = PendingReplies()
        val gone = replies.register(1)!!
        replies.remove(1)
        replies.end()
        assertFalse(gone.isCompleted)
    }
}
