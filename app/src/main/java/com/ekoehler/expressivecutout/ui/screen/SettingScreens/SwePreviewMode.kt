package com.ekoehler.expressivecutout.ui.screen

/** What [SwePreviewCard] draws around the camera dot. */
internal enum class SwePreviewMode {
    /** The black resting pill with its widgets, as it shows on the real screen. */
    PILL,

    /** No pill, only an outline of the invisible tap area it leaves behind. */
    HIDDEN_AREA,

    /** The bare camera dot, for when the resting pill is off. */
    DOT,
}
