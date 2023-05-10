package com.h4pay.store.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import com.h4pay.store.*
import com.h4pay.store.databinding.FragmentVoucherBinding
import com.h4pay.store.model.Product
import com.h4pay.store.model.Voucher
import com.h4pay.store.recyclerAdapter.itemsRecycler
import com.h4pay.store.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.lang.Exception
import java.util.*

class VoucherFragment : Fragment() {
    private lateinit var view: FragmentVoucherBinding
    private lateinit var recyclerAdapter: itemsRecycler
    private lateinit var voucherId: String
    private val viewModel: VoucherViewModel by viewModels()
    private var item = JsonObject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        ISODateFormat.timeZone = TimeZone.getTimeZone("UTC")
        KoreanDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        view = DataBindingUtil.inflate(inflater, R.layout.fragment_voucher, container, false)
        return view.root
    }

    override fun onStart() {
        super.onStart()
        initUI()
        view.lifecycleOwner = requireActivity()
        val passedId: String? = requireArguments()["orderId"] as String?

        lifecycleScope.launch {
            viewModel.voucherDetailState.collect(voucherDetailCollector)
        }
        lifecycleScope.launch {

            viewModel.exchangeVoucherResultState.collect(exchangeVoucherResultCollector)
        }

        if (passedId != null) {
            loadVoucherDetail(passedId)
        }
    }

    private val voucherDetailCollector by lazy {
        CustomFlowCollector<Voucher?>(requireContext(),
            {
                customDialogs.yesOnlyDialog(requireContext(), "금액권 정보를 불러오지 못했습니다.", {}, "오류", null)
            }, {
                if (it == null) {
                    Toast.makeText(
                        requireActivity(),
                        "상품권 정보를 불러올 수 없어요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@CustomFlowCollector
                } else {
                    loadVoucherDetail(it)
                    voucherId = it.id
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            view.idInput.setText(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun loadVoucherDetail(
        voucher: Voucher
    ) {
        view.orderUid.text = voucher.receiver.tel
        view.orderDate.text = KoreanDateFormat.format(voucher.date)
        view.orderExpire.text = KoreanDateFormat.format(voucher.expire)
        view.orderExchanged.text = if (voucher.exchanged) "교환 됨" else "교환 안됨"
        val backgroundDrawable =
            if (voucher.exchanged) R.drawable.rounded_red else R.drawable.rounded_green
        view.orderExchanged.background =
            ContextCompat.getDrawable(requireContext(), backgroundDrawable)

        view.voucherAmount.text = "상품권 금액: ${moneyFormat.format(voucher.amount)} 원"
        if (!voucher.exchanged) {
            view.productArea.visibility = View.VISIBLE
        }
        view.exchangeButton.setOnClickListener {
            exchange(voucher)
        }
        view.exchangeButton.isVisible = true
    }


    private fun initUI() {
        view.switchToPurchase.setOnClickListener {
            swapFragment(requireActivity(), FragmentType.Purchase, Bundle())
        }
        view.cameraScan.setOnClickListener {
            initScan(this)
        }
        view.cameraScanCircle.setOnClickListener {
            initScan(this)
        }
        view.clearId.setOnClickListener {
            view.idInput.setText("")
        }
        view.clearText.setOnClickListener {
            view.productBarcode.setText("")
        }

        view.root.viewTreeObserver.addOnGlobalLayoutListener {
            // View의 focus가 변경됐을 때를 observe.
            if (isAdded)
                if (isOnScreenKeyboardEnabled(view.root, resources.configuration)) {
                    Log.d("VoucherFragment", "keyboard enabled")
                    openImm(requireActivity(), false)
                }

        }

        view.idInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
                Log.d("TAG", barcode.startsWith("3").toString())
                if (barcode.length == 25) {
                    if (!barcode.startsWith("3")) {
                        val bundle = Bundle()
                        bundle.putString("orderId", barcode)
                        swapFragment(requireActivity(), FragmentType.Purchase, bundle)
                        return
                    }
                    Log.d("BARCODE", barcode)
                    viewModel.getVoucherDetail(barcode)

                    p0!!.clear()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        view.productBarcode.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(p0: Editable?) {
                val barcode = p0.toString()
                if (barcode.length == 13) {
                    val product = findProductByBarcode(barcode)
                    if (product == null) { // 바코드로 상품을 찾아 없으면
                        Toast.makeText(
                            requireActivity(),
                            "바코드로 제품 정보를 찾을 수 없어요. 정확히 스캔했는지 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        p0!!.clear()
                        return // 오류 메시지 표시 후 리턴
                    }
                    view.productArea.visibility = View.VISIBLE

                    addProductToItem(product.id)
                    if (!view.itemsRecyclerView.isActivated) { // RecyclerView가 활성화 되지 않았으면
                        initRecyclerView() // init한다.
                    } else { // 활성화 되어 있으면
                        recyclerAdapter.changeItems(itemJsonToArray(item)) // 어댑터의 아이템을 모두 변경해준다.
                    }
                    p0!!.clear()
                    val totalAmount = calcTotalAmount()

                    view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(totalAmount)} 원"
                    Thread {
                        Thread.sleep(100)
                        requireActivity().runOnUiThread {
                            view.productBarcode.requestFocus()
                        }
                    }.start() //editText focus in
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }


    private fun loadVoucherDetail(passedId: String) {
        voucherId = passedId
        view.idInput.setText(passedId)
        viewModel.getVoucherDetail(passedId)
    }

    private fun initRecyclerView() {
        val lm = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        val recyclerView = view.itemsRecyclerView

        recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            // use a linear layout manager
            layoutManager = lm
            // specify an viewAdapter (see also next example)
            val recycler = itemsRecycler(true, requireActivity(), itemJsonToArray(item))
            recycler.setOnRecyclerDataChanged { onRecyclerDataChanged() }
            recyclerAdapter = recycler
            adapter = recycler

            isVisible = true
            post {
                view.productBarcode.isFocusableInTouchMode = true;
                view.productBarcode.requestFocus()


            }
        }
    }

    private fun unLoadVoucher() {
        view.orderUid.text = ""
        view.orderDate.text = ""
        view.orderExpire.text = ""
        view.orderExchanged.text = ""
        val backgroundDrawable = R.color.white
        view.orderExchanged.background =
            ContextCompat.getDrawable(requireContext(), backgroundDrawable)
        view.voucherAmount.text = ""
        view.productArea.visibility = View.INVISIBLE
        recyclerAdapter.changeItems(JsonArray())
        onRecyclerDataChanged()
        view.exchangeButton.isVisible = false
    }

    fun findProductByBarcode(barcode: String): Product? {
        for (i in prodList.indices) {
            try {
                if (prodList[i].barcode == barcode) {
                    return prodList[i]
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    override fun onDetach() {
        super.onDetach()
        Log.e("TAG", "detach")
    }

    fun addProductToItem(productId: Int) {
        if (item.has(productId.toString())) { // 해당 제품이 존재하면
            val qty: Int = item[productId.toString()].asInt // 해당 제품의 수를 가져와
            item.addProperty(productId.toString(), qty + 1) // 1을 더한 것을 저장한다
        } else {
            item.addProperty(productId.toString(), 1) // 그 외의 경우에는 해당 제품의 개수를 1로 지정한다
        }
        return
    }

    fun calcTotalAmount(): Int {
        var totalAmount: Int = 0;
        item.keySet().forEach { // 모든 추가된 품목들에 대해
            for (j in prodList.indices) {
                if (prodList[j].id == Integer.parseInt(it)) { // 제품 배열에서 id가 일치하는 것을 찾아
                    totalAmount += item[it].asInt * prodList[j].price // 제품의 가격과 해당 제품이 담긴 갯수를 곱해 totalAmount에 더한다
                }
            }
        }
        return totalAmount
    }

    private fun exchange(voucher: Voucher) {
        val totalAmount = calcTotalAmount()

        when {
            totalAmount > voucher.amount -> { // 선택한 제품 금액 총합보다 액면가가 작으면
                // 교환이 불가하다는 메시지를 띄운다.
                Toast.makeText(
                    requireActivity(),
                    "액면가보다 선택한 제품의 총 금액이 더 많습니다. 제거해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            totalAmount < voucher.amount * 0.6 -> { // 선택한 제품 금액 총합이 액면가의 60%보다 작으면
                // 교환이 불가하다는 메시지를 띄운다.
                Toast.makeText(requireActivity(), "액면가의 60% 이상을 사용해야 합니다.", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            else -> {
                viewModel.exchangeVoucher(voucher.id, item)
            }
        }
    }

    private val exchangeVoucherResultCollector by lazy {
        CustomFlowCollector<Boolean>(requireContext(),
            {
                Toast.makeText(requireActivity(), "교환 처리에 실패했습니다.", Toast.LENGTH_SHORT)
                    .show()
                unLoadVoucher()
            },
            {
                Toast.makeText(requireActivity(), "교환 처리에 성공했습니다.", Toast.LENGTH_SHORT)
                    .show()
                unLoadVoucher()
            })
    }

    private fun onRecyclerDataChanged() {
        item = itemArrayToJson(recyclerAdapter.getItems())
        Log.d("RECYCLER", item.toString())
        view.totalAmount.text = "현재 사용 금액: ${moneyFormat.format(calcTotalAmount())} 원"
    }
}