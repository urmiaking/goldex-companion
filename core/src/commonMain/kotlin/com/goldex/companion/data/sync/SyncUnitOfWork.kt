package com.goldex.companion.data.sync

/** Keeps invoice, ledger, balances and outbox in one atomic financial operation. */
interface SyncUnitOfWork { fun <T> transaction(action: () -> T): T }
