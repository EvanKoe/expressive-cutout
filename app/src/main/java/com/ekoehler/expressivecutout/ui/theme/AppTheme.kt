package com.ekoehler.expressivecutout.ui.theme

import androidx.annotation.StringRes
import com.ekoehler.expressivecutout.R

/** The user's chosen colour scheme. [SYSTEM] follows the device's light/dark setting. */
enum class AppTheme(@param:StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}
