package com.goldex.companion.data.local

import com.goldex.companion.data.sync.SyncCheckpoint
import com.goldex.companion.data.sync.SyncDao
import com.goldex.companion.domain.onboarding.OnboardingCheckpoint

/** Reuses the existing marker; neither the schema nor its stored identifier changes. */
class RoomOnboardingCheckpoint(private val dao: SyncDao) : OnboardingCheckpoint {
    override fun hasSeededOpeningInventory(): Boolean = dao.marker(MARKER_ID) != null
    override fun markOpeningInventorySeeded() = dao.checkpoint(SyncCheckpoint(id = MARKER_ID))

    private companion object { const val MARKER_ID = "onboarding-complete" }
}
