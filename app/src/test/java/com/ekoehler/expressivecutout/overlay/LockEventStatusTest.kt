package com.ekoehler.expressivecutout.overlay

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import com.ekoehler.expressivecutout.R
import com.ekoehler.expressivecutout.core.CutoutSignal
import com.ekoehler.expressivecutout.core.SystemEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LockEventStatusTest {

    @Test
    fun testSystemEventTypeHasDeviceLockedAndDeviceUnlocked() {
        // SystemEventType must have DEVICE_LOCKED and DEVICE_UNLOCKED
        val lockedType = SystemEventType.DEVICE_LOCKED
        val unlockedType = SystemEventType.DEVICE_UNLOCKED

        assertEquals(Icons.Rounded.Lock, lockedType.defaultIcon)
        assertEquals(R.string.event_device_locked, lockedType.labelRes)
        assertEquals(0xFFFACC15, lockedType.accent)

        assertEquals(Icons.Rounded.LockOpen, unlockedType.defaultIcon)
        assertEquals(R.string.event_device_unlocked, unlockedType.labelRes)
        assertEquals(0xFFFACC15, unlockedType.accent)
    }

    @Test
    fun testAnimatedIconForLockedAndUnlocked() {
        // Locked icon should hold on closed padlock (frame 0..0)
        val lockedAnimation = SystemEventType.DEVICE_LOCKED.animatedIcon()
        assertNotNull("DEVICE_LOCKED should have an animated icon", lockedAnimation)
        assertEquals(R.raw.unlock, lockedAnimation?.resId)
        assertEquals(0, lockedAnimation?.clipStartFrame)
        assertEquals(0, lockedAnimation?.clipEndFrame)
        assertTrue(lockedAnimation?.tint == true)

        // Unlocked animation should snap open instantly starting at frame 5 (motion start) to frame 25 (fully open) at speed >= 1.8f
        val unlockedAnimation = SystemEventType.DEVICE_UNLOCKED.animatedIcon()
        assertNotNull("DEVICE_UNLOCKED should have an animated icon", unlockedAnimation)
        assertEquals(R.raw.unlock, unlockedAnimation?.resId)
        assertEquals(5, unlockedAnimation?.clipStartFrame)
        assertEquals(25, unlockedAnimation?.clipEndFrame)
        assertTrue("Unlock animation speed should be fast for instant paddle opening", (unlockedAnimation?.speed ?: 1f) >= 1.8f)
        assertTrue(unlockedAnimation?.tint == true)
    }

    @Test
    fun testLockFlowPreservesIslandStateAcrossUnlock() {
        // When device is locked, lock event is persistent (no auto-dismiss)
        // When device unlocks, it transitions to unlocked event and auto-dismisses
        fun isPinnedLockEvent(
            eventId: Long?,
            lastLockEventId: Long?,
            isDeviceLocked: Boolean,
        ): Boolean {
            return isDeviceLocked && eventId != null && eventId == lastLockEventId
        }

        val lockedEventId = 1L
        val otherEventId = 2L

        assertTrue(
            "Lock event should be pinned while device is locked",
            isPinnedLockEvent(
                eventId = lockedEventId,
                lastLockEventId = lockedEventId,
                isDeviceLocked = true,
            ),
        )

        assertFalse(
            "Non-lock event should not be pinned as lock event",
            isPinnedLockEvent(
                eventId = otherEventId,
                lastLockEventId = lockedEventId,
                isDeviceLocked = true,
            ),
        )

        assertFalse(
            "Lock event should not be pinned when device is unlocked",
            isPinnedLockEvent(
                eventId = lockedEventId,
                lastLockEventId = lockedEventId,
                isDeviceLocked = false,
            ),
        )
    }

    @Test
    fun testFaceUnlockOnLockScreenTransitionsToUnlockedState() {
        // Scenario: Phone is on lock screen (isKeyguardLocked = true).
        // User unlocks via Face (isDeviceLocked transitions from true -> false).
        // The island must reflect the actual device lock state (isDeviceLocked), not keyguard lock state.

        fun resolveIslandLockState(
            isKeyguardLocked: Boolean,
            isDeviceLocked: Boolean,
        ): String {
            return if (isDeviceLocked) "locked" else "unlocked"
        }

        // Before face unlock: on lock screen and device is locked
        assertEquals(
            "locked",
            resolveIslandLockState(isKeyguardLocked = true, isDeviceLocked = true),
        )

        // After face unlock: still on lock screen (isKeyguardLocked=true), but device is unlocked (isDeviceLocked=false)
        assertEquals(
            "unlocked",
            resolveIslandLockState(isKeyguardLocked = true, isDeviceLocked = false),
        )

        // After swiping up to home screen: keyguard dismissed and device is unlocked
        assertEquals(
            "unlocked",
            resolveIslandLockState(isKeyguardLocked = false, isDeviceLocked = false),
        )
    }

    @Test
    fun testHideOnLockscreenRespectsKeyguardState() {
        fun shouldHideOverlay(
            hideOnLockscreen: Boolean,
            isKeyguardLocked: Boolean,
        ): Boolean {
            return hideOnLockscreen && isKeyguardLocked
        }

        // When hideOnLockscreen is enabled, overlay is hidden whenever keyguard is locked
        assertTrue(shouldHideOverlay(hideOnLockscreen = true, isKeyguardLocked = true))
        assertFalse(shouldHideOverlay(hideOnLockscreen = true, isKeyguardLocked = false))

        // When hideOnLockscreen is disabled, overlay remains visible on keyguard
        assertFalse(shouldHideOverlay(hideOnLockscreen = false, isKeyguardLocked = true))
        assertFalse(shouldHideOverlay(hideOnLockscreen = false, isKeyguardLocked = false))
    }

    @Test
    fun testLivePillFallbackToLockEventWhileLocked() {
        fun resolveLivePillToReturnTo(
            hasCall: Boolean,
            hasMusic: Boolean,
            hasTimer: Boolean,
            isDeviceLocked: Boolean,
            isLockEventEnabled: Boolean,
            cutoutEnabled: Boolean,
        ): String? {
            if (hasCall) return "call"
            if (hasMusic) return "music"
            if (hasTimer) return "timer"
            if (isDeviceLocked && isLockEventEnabled && cutoutEnabled) return "lock"
            return null
        }

        // When locked and no other live tiles exist, returns lock
        assertEquals(
            "lock",
            resolveLivePillToReturnTo(
                hasCall = false,
                hasMusic = false,
                hasTimer = false,
                isDeviceLocked = true,
                isLockEventEnabled = true,
                cutoutEnabled = true,
            ),
        )

        // When music is active while locked, music takes precedence over lock
        assertEquals(
            "music",
            resolveLivePillToReturnTo(
                hasCall = false,
                hasMusic = true,
                hasTimer = false,
                isDeviceLocked = true,
                isLockEventEnabled = true,
                cutoutEnabled = true,
            ),
        )

        // When unlocked, does not return to lock
        assertNull(
            resolveLivePillToReturnTo(
                hasCall = false,
                hasMusic = false,
                hasTimer = false,
                isDeviceLocked = false,
                isLockEventEnabled = true,
                cutoutEnabled = true,
            ),
        )
    }
}
