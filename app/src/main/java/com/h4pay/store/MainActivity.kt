package com.h4pay.store

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.h4pay.store.databinding.ActivityMainBinding
import com.h4pay.store.fragments.CustomFlowCollector
import com.h4pay.store.fragments.PurchaseFragment
import com.h4pay.store.fragments.VoucherFragment
import com.h4pay.store.model.Product
import com.h4pay.store.util.currentFragmentType
import com.h4pay.store.util.swapFragment
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

lateinit var prodList: List<Product>

object FragmentTag {
    const val PURCHASE = "PURCHASE"
    const val VOUCHER = "VOUCHER"
}

// At the top level of your kotlin file:
//val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "store")

fun showServerError(context: Activity) {
    Log.e("Error", "Error occured in ${context.localClassName}")
    AlertDialog.Builder(context, R.style.AlertDialogTheme)
        .setTitle("서버 오류")
        .setMessage("서버 오류로 인해 사용할 수 없습니다. 개발자에게 문의 바랍니다.")
        .setPositiveButton("확인") { _: DialogInterface, _: Int ->
            context.finish()
        }.show()
}

enum class FragmentType {
    Purchase,
    Voucher,
    Closed
}

val ISODateFormat = SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.JAPANESE
)
val KoreanDateFormat = SimpleDateFormat(
    "yyyy년 MM월 dd일 HH시 mm분 ss초", Locale.KOREAN
)
val moneyFormat: NumberFormat = DecimalFormat("#,###")

var storeStatus: Boolean = false

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var view: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()


    private val storeStatusCollector = CustomFlowCollector<Boolean>(this, {
        customDialogs.yesOnlyDialog(this, "매점 개점 여부를 불러오지 못했습니다.", {}, "오류", null)
    }) {
        setStoreStatus(it ?: false)
    }

    private fun fetchStoreStatus() {
        viewModel.getStoreStatus()
    }

    private fun setStoreStatus(isOpened: Boolean) {
        view.openStatus.isChecked = isOpened
        view.openStatusText.text = if (isOpened) "OPEN" else "CLOSED"
        if (!isOpened) swapFragment(this, FragmentType.Closed, Bundle()) // 닫기
        else swapFragment(
            this,
            currentFragmentType,
            Bundle()
        ) // Closed를 제외한 것만 Type만 넣는 "currentFragmentType"으로 Fragment 변경.
    }

    private fun initUI() {
        view.openStatus.setOnCheckedChangeListener { _, isOpened ->
            viewModel.changeStoreStatus(isOpened)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Toast.makeText(this, token, Toast.LENGTH_SHORT).show()
        currentFragmentType = FragmentType.Purchase
        view = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        supportFragmentManager.beginTransaction().replace(R.id.fragment_view, PurchaseFragment())
            .commit()

        view.lifecycleOwner = this

        lifecycleScope.launch {
            viewModel.storeStatus.collect(storeStatusCollector)
        }
        initUI()
        fetchStoreStatus()
    }

}