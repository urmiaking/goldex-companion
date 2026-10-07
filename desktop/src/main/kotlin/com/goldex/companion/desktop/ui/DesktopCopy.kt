package com.goldex.companion.desktop.ui

/** For application-authored validation errors only; never pass customer input or stored labels. */
internal fun programErrorText(text: String): String = text.replace("\u0621", "").replace("\u0654", "")
