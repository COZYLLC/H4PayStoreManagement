package com.h4pay.store.util

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.FragmentType
import com.h4pay.store.R
import com.h4pay.store.fragments.ClosedFragment
import com.h4pay.store.fragments.PurchaseFragment
import com.h4pay.store.fragments.VoucherFragment

lateinit var currentFragmentType: FragmentType


fun openImm(context: Activity, guide: Boolean) { // guide 값에 따라 가상 키보드를 켤지 끌지를 알려줌. (true -> 켜라)
    val inputMethodManager: InputMethodManager? =
        ContextCompat.getSystemService(context, InputMethodManager::class.java)
    if (inputMethodManager != null) {
        inputMethodManager.showInputMethodPicker()
        val message = if (guide) "\"스크린 키보드\" 옵션이 켜져있어 가상 키보드가 올라옵니다. 해당 옵션을 꺼주세요." else "\"스크린 키보드\" 옵션이 꺼져있어 가상 키보드가 올라오지 않습니다. 해당 옵션을 켜주세요."
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

}

fun initScan(context: Activity) {
    val intentIntegrator = IntentIntegrator(context)
    intentIntegrator.initiateScan()
}


fun swapFragment(context: FragmentActivity, fragmentType: FragmentType, data: Bundle) {
    val fragment: Fragment = when (fragmentType) {
        FragmentType.Closed -> {
            ClosedFragment()
        }
        FragmentType.Purchase -> {
            PurchaseFragment()
        }
        FragmentType.Voucher -> {
            VoucherFragment()
        }
    }
    Log.e("FragmentUtil", fragment.toString())

    if (fragmentType != FragmentType.Closed) { // 닫혀 있을 때 띄우는 fragment는 상태에 저장하지 않는다.
        currentFragmentType = fragmentType
    }

    fragment.arguments = data
    val transaction: FragmentTransaction = context.supportFragmentManager.beginTransaction()
    transaction.replace(R.id.fragment_view, fragment)
    transaction.commit()

}
