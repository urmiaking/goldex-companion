package com.goldex.companion

import android.app.Application
import com.goldex.companion.app.AndroidAppContainer

/** Owns process-lifetime adapters; feature state stays in lifecycle ViewModels. */
class GoldexApplication : Application() {
    val container: AndroidAppContainer by lazy { AndroidAppContainer(this) }
}
