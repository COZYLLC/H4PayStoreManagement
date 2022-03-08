package com.h4pay.store.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.*
import com.h4pay.store.databinding.FragmentPurchaseBinding
import com.h4pay.store.model.Purchase
import com.h4pay.store.networking.H4PayService
import com.h4pay.store.networking.initService
import com.h4pay.store.networking.tools.networkInterceptor
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat

class PurchaseFragment : Fragment() {

    private lateinit var h4payService: H4PayService
    private lateinit var view: FragmentPurchaseBinding
    private val TAG = "PurchaseFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e("PurchaseFragment", "created")
        h4payService = initService()
        fetchProduct()
        view = DataBindingUtil.inflate(inflater, R.layout.fragment_purchase, container, false)

        return view.root
    }

    private fun setExchangeButtonListener(orderId: String) {
        //------Cancel and Exchange Button OnClick Event----------
        val context = this.requireContext()
        view.exchangeButton.setOnClickListener {
            customDialogs.yesNoDialog(context, "확인", "정말로 교환처리 하시겠습니까?", {
                val requestBody = JsonObject()
                val orderIdArray = JsonArray()
                orderIdArray.add(orderId)
                requestBody.add("orderId", orderIdArray)
                if (isGift(orderId) == true) { //선물인 경우
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            h4payService.exchangeGift(requestBody)
                        }.onSuccess {
                            exchangeSuccess(true)
                        }.onFailure {
                            Log.e(TAG, it.message!!)
                            showServerError(requireActivity())
                            return@launch
                        }
                    }
                } else { //선물이 아닌 경우
                    lifecycleScope.launch {
                        kotlin.runCatching {
                            h4payService.exchangeOrder(requestBody)
                        }.onSuccess {
                            exchangeSuccess(true)
                        }.onFailure {
                            Log.e(TAG, it.message!!)
                            showServerError(requireActivity())
                            return@launch
                        }
                    }
                }
            }, {})
        }
    }

    private fun processIntentOrderId(passedOrderId: String) {
        if (passedOrderId.length != 25) return
        if (isGift(passedOrderId) == true) {
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.getGiftDetail(passedOrderId)
                }.onSuccess {
                    if (it.size == 1)
                        loadOrderDetail(it[0])
                }.onFailure {
                    Toast.makeText(
                        requireActivity(),
                        "주문 정보를 불러올 수 없습니다.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        } else {
            lifecycleScope.launch {
                kotlin.runCatching {
                    h4payService.getOrderDetail(passedOrderId)
                }.onSuccess {
                    if (it.size == 1)
                        loadOrderDetail(it[0])
                }.onFailure {
                    Toast.makeText(
                        requireActivity(),
                        "주문 정보를 불러올 수 없습니다.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi()
        if (arguments != null) {
            val passedId: String? = requireArguments()["orderId"] as String?
            if (passedId != null)
                processIntentOrderId(passedId)
        }
    }


    private fun initUi() {
        view.cameraScan.setOnClickListener {
            initScan(requireActivity())
        }
        view.clearId.setOnClickListener {
            view.orderIdInput.setText("")
        }
        view.cameraScanCircle.setOnClickListener {
            initScan(requireActivity())
        }
        view.callDeveloper.setOnClickListener {
            val intent = Intent(this.requireContext(), CallDeveloper::class.java)
            startActivity(intent)
        }
        view.showInfo.setOnClickListener {
            val intent = Intent(this.requireContext(), H4PayInfo::class.java)
            startActivity(intent)
        }

        view.switchToVoucher.setOnClickListener {
            swapFragment(requireActivity(), FragmentType.Voucher, Bundle())
        }

        view.goToDashboard.setOnClickListener {
            val intent = Intent()
            intent.data = Uri.parse("https://manager.h4pay.co.kr")
            startActivity(intent)
        }

        view.exchangeButton.isVisible = false
        val inputMethodManager: InputMethodManager? =
            getSystemService(requireContext(), InputMethodManager::class.java)
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.orderIdInput.windowToken, 0)
            view.root.viewTreeObserver.addOnGlobalLayoutListener {
                // View의 focus가 변경됐을 때를 observe.
                if (isAdded) {
                    if (isOnScreenKeyboardEnabled(
                            activity?.findViewById<FrameLayout>(R.id.fragment_view)!!.rootView,
                            resources.configuration
                        )
                    ) {
                        Log.d("PurchaseFragment", "keyboard enabled")
                        openImm(requireActivity())
                    }

                }
            }
        }
        view.orderIdInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(editable: Editable) {
                // 입력이 끝났을 때
                val inputtedOrderId = editable.toString()
                view.orderIdInput.requestFocus();
                if (inputtedOrderId.length < 0 || inputtedOrderId.length > 25) {
                    Toast.makeText(requireActivity(), "올바른 주문번호가 아닙니다!", Toast.LENGTH_SHORT)
                        .show()
                } else if (inputtedOrderId.length == 25) {
                    if (inputtedOrderId.startsWith("3")) { // Voucher
                        val bundle = Bundle()
                        bundle.putString("orderId", inputtedOrderId)
                        swapFragment(requireActivity(), FragmentType.Voucher, bundle)
                        return
                    }
                    //Handling Numbers
                    val f = NumberFormat.getInstance()
                    f.isGroupingUsed = false

                    when {
                        isGift(inputtedOrderId) == false -> { // general order
                            lifecycleScope.launch {
                                kotlin.runCatching {
                                    h4payService.getOrderDetail(inputtedOrderId)
                                }.onSuccess {
                                    if (it.size == 1)
                                        loadOrderDetail(it[0])
                                }.onFailure {
                                    Toast.makeText(
                                        requireActivity(),
                                        "주문 내역을 불러올 수 없습니다!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                            //API CALL
                        }
                        isGift(inputtedOrderId) == true -> { // gift
                            lifecycleScope.launch {
                                kotlin.runCatching {
                                    h4payService.getGiftDetail(inputtedOrderId)
                                }.onSuccess {
                                    if (it.size == 1)
                                        loadOrderDetail(it[0])
                                }.onFailure {
                                    Toast.makeText(
                                        requireActivity(),
                                        "주문 내역을 불러올 수 없습니다!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                            }
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                "올바른 주문번호가 아닙니다! 1 혹은 2로 시작해야 합니다!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        })
    }

    fun isGift(input: String): Boolean? {
        return if (input.startsWith("1") || input.startsWith("2")) input.startsWith("2") else null
    }

    private fun setButton(exchanged: Boolean) {
        view.exchangeButton.isVisible = !exchanged
        view.exchangeButton.isEnabled = !exchanged
    }

    private fun fetchProduct() {
        lifecycleScope.launch {
            kotlin.runCatching {
                h4payService.getProducts()
            }.onSuccess {
                prodList = it
            }.onFailure {
                Log.e(TAG, it.message!!)
                showServerError(requireActivity())
                return@launch
            }
        }
    }

    fun loadOrderDetail(
        purchase: Purchase
    ) {
        view.orderUid.text = purchase.uid ?: view.orderUid.text
        view.orderDate.text = KoreanDateFormat.format(purchase.date) ?: view.orderDate.text
        view.orderExpire.text =
            KoreanDateFormat.format(purchase.expire) ?: view.orderExpire.text
        view.orderAmount.text = "${moneyFormat.format(purchase.amount)} 원"
        if (purchase.exchanged) {
            view.orderExchanged.text = "교환 됨"
            view.orderExchanged.setTextColor(Color.WHITE)
            view.orderExchanged.background =
                ContextCompat.getDrawable(requireActivity(), R.drawable.rounded_red)
            view.orderIdInput.requestFocus()
            setButton(purchase.exchanged)
        } else if (!purchase.exchanged) {
            view.orderExchanged.text = "교환 안됨"
            view.orderExchanged.setTextColor(Color.BLACK)
            view.orderExchanged.background =
                ContextCompat.getDrawable(requireActivity(), R.drawable.rounded_green)
            view.orderIdInput.requestFocus()
            setButton(purchase.exchanged)
        }
        var itemObject = purchase.item // stash item array
        val itemArray = itemJsonToArray(itemObject)
        recyclerViewInit(itemArray)
        setExchangeButtonListener(purchase.orderId)
        view.orderIdInput.setText("")
    }

    private fun recyclerViewInit(itemArray: JsonArray) {
        val lm = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        view.itemsRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = lm
            val recycler = itemsRecycler(false, requireActivity(), itemArray)
            adapter = recycler
        }

        view.itemsRecyclerView.isVisible = true

        view.itemsRecyclerView.post {
            view.orderIdInput.isFocusableInTouchMode = true;
            view.orderIdInput.requestFocus()
        } //RecyclerView focus release

        lifecycleScope.launch {
            Thread.sleep(1000)
            view.orderIdInput.requestFocus()
        } //view.orderIdInputText focus in
    }

    private fun exchangeSuccess(status: Boolean) {
        if (status) {
            Toast.makeText(requireActivity(), "교환이 정상적으로 완료되었습니다!", Toast.LENGTH_SHORT).show()
            makeEmpty()
        } else {
            Toast.makeText(
                requireActivity(),
                "교환에 실패했습니다.\n이미 교환되었거나 없는 주문번호입니다.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun makeEmpty() {
        view.orderExchanged.text = ""
        view.orderAmount.text = ""
        view.orderExpire.text = ""
        view.orderDate.text = ""
        view.orderUid.text = ""
        view.orderExchanged.setBackgroundColor(Color.TRANSPARENT)

        view.exchangeButton.isVisible = false
        try {
            view.itemsRecyclerView.isVisible = false
        } catch (e: UninitializedPropertyAccessException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            view.orderIdInput.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


}