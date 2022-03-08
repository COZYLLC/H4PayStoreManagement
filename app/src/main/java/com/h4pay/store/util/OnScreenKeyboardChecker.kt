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

    val deviceHeight = view.rootView.height
    val viewHeight = r.height()
    Log.d("OnScreenKeyboard", "${deviceHeight} | ${viewHeight}, ${deviceHeight-viewHeight} ")
    return deviceHeight-viewHeight >200
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