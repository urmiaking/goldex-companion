package com.goldex.companion.platform

fun interface IdGenerator {
    fun newId(): String
}

/** Generates the existing UUID string shape; saved identifiers are never regenerated. */
expect object RandomIdGenerator : IdGenerator {
    override fun newId(): String
}
