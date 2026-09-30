package com.goldex.companion.platform

import java.util.UUID

actual object RandomIdGenerator : IdGenerator {
    actual override fun newId(): String = UUID.randomUUID().toString()
}
