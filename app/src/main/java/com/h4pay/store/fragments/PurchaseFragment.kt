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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.h4pay.store.*
import com.h4pay.store.databinding.FragmentPurchaseBinding
import com.h4pay.store.model.Product
import com.h4pay.store.model.Purchase
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.*
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.internal.http2.StreamResetException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.NumberFormat
import javax.net.ssl.SSLHandshakeException

inline fun <reified T : Throwable> Result<*>.except(): Result<*> =
    onFailure { if (it is T) throw it }

fun <T> CustomFlowCollector(
    context: Context,
    errorHandler: (Throwable) -> Unit,
    successHandler: (T?) -> Unit
): FlowCollector<State<T>> {
    return FlowCollector { value ->
        if (value is State.Error) {
            // Todo: Handle Global Error (Network Error, Login Error, ...)
            when (value.error) {
                is SocketTimeoutException,
                is UnknownHostException,
                is SSLHandshakeException,
                is StreamResetException,
                is ConnectException -> {
                    Toast.makeText(context, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    errorHandler(value.error)
                }
            }
        }
        if (value is State.Success) {
            successHandler(value.data)
        }
    }
}

class PurchaseFragment : Fragment() {

    private lateinit var view: FragmentPurchaseBinding
    private val viewModel: PurchaseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        view = DataBindingUtil.inflate(inflater, R.layout.fragment_purchase, container, false)

        return view.root
    }

    private fun setExchangeButtonListener(orderId: String) {
        //------Cancel and Exchange Button OnClick Event----------
        val context = this.requireContext()
        view.exchangeButton.setOnClickListener {
            customDialogs.yesNoDialog(context, "확인", "정말로 교환처리 하시겠습니까?", {
                val orderIds = arrayListOf(orderId)
                if (isGift(orderId) == true) { //선물인 경우
                    viewModel.exchangeGift(orderIds)
                } else { //선물이 아닌 경우
                    viewModel.exchangeOrder(orderIds)
                }
            }, {})
        }
    }

    private val purchaseDetailCollector by lazy {
        CustomFlowCollector<Purchase?>(requireActivity(), {
            customDialogs.yesOnlyDialog(requireContext(), "주문내역 조회 중 오류가 발생했습니다.", {}, "오류", null)
        }) { data ->
            H4PayLogger.d(this, data.toString())
            if (data == null) return@CustomFlowCollector
            loadOrderDetail(data)
        }
    }


    private val exchangeResultCollector by lazy {
        CustomFlowCollector<Boolean>(requireContext(),
            {
                Toast.makeText(requireActivity(), "교환 처리에 실패했습니다.", Toast.LENGTH_SHORT)
                    .show()
                makeEmpty()
            },
            {
                Toast.makeText(requireActivity(), "교환 처리에 성공했습니다.", Toast.LENGTH_SHORT)
                    .show()
                makeEmpty()
            })
    }

    private fun processIntentOrderId(passedOrderId: String) {
        if (passedOrderId.length != 25) return
        if (isGift(passedOrderId) == true) {
            viewModel.getGiftDetail(passedOrderId)
        } else {
            viewModel.getOrderDetail(passedOrderId)
        }
    }

    override fun onStart() {
        super.onStart()
        fetchProduct()
        initUi()

        view.lifecycleOwner = requireActivity()
        lifecycleScope.launch {
            viewModel.purchaseDetailState.collect(purchaseDetailCollector)
        }
        lifecycleScope.launch {
            viewModel.productListState.collect(productsCollector)
        }
        lifecycleScope.launch {
            viewModel.exchangeState.collect(exchangeResultCollector)
        }

        if (arguments != null) {
            val passedId: String? = requireArguments()["orderId"] as String?
            if (passedId != null)
                processIntentOrderId(passedId)

        }
    }

    private fun initUi() {
        view.cameraScan.setOnClickListener {
            initScan(this)

        }
        view.clearId.setOnClickListener {
            view.orderIdInput.setText("")
        }
        view.cameraScanCircle.setOnClickListener {
            initScan(this)
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
                        openImm(requireActivity(), false)
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
                Log.i("OrderId", inputtedOrderId)

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
                            viewModel.getOrderDetail(inputtedOrderId)
                        }
                        isGift(inputtedOrderId) == true -> { // gift
                            viewModel.getGiftDetail(inputtedOrderId)
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

    private val productsCollector by lazy {
        CustomFlowCollector<List<Product>>(requireContext(), {
            customDialogs.yesOnlyDialog(requireContext(), "주문내역 조회 중 오류가 발생했습니다.", {}, "오류", null)
        }) {
            prodList = it ?: listOf()
        }
    }

    private fun fetchProduct() {
        viewModel.getProducts()
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
        val scanningResult: IntentResult? =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (scanningResult != null && !scanningResult.contents.isNullOrEmpty()) {
            view.orderIdInput.setText(scanningResult.contents)
        } else {
            Toast.makeText(activity, "Nothing scanned", Toast.LENGTH_SHORT).show()
        }
    }


}