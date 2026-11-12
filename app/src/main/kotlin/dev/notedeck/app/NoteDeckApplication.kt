package dev.notedeck.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Creates Hilt's app-wide component. It replaces the hand-written AppContainer from the first half of M3. */
@HiltAndroidApp
class NoteDeckApplication : Application()
