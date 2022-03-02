package com.h4pay.store.util

import android.content.res.Configuration
import android.graphics.Rect
import android.util.Log
import android.view.View

 fun isHardwareKeyConnected(configuration: Configuration): Boolean {
    return configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.keyboard != Configuration.KEYBOARD_UNDEFINED
}

 fun isKeyboardOpened(view:View) : Boolean{
    val r: Rect = Rect()
    view.getWindowVisibleDisplayFrame(r)

    val screenHeight = view.rootView.height
    val rectHeight = r.height()
    Log.d("OnScreenKeyboard", "${screenHeight} | ${rectHeight}")
    return screenHeight - rectHeight > 100
}

fun isOnScreenKeyboardEnabled(view:View, configuration: Configuration) : Boolean{
    return if (isKeyboardOpened(view)) {
        true
    } else if ( !isKeyboardOpened(view)) {
        false
    } else {
        false
    }
}