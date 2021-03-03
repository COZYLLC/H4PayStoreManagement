package com.h4pay.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.h4pay.store.customDialogs.yesNoDialog
import com.h4pay.store.networking.cancelOrder
import com.h4pay.store.networking.getProdList
import com.h4pay.store.networking.productStateChanger
import com.h4pay.store.networking.tools.rmProductClass
import com.h4pay.store.networking.tools.uploadImage
import com.h4pay.store.recyclerAdapter.RecyclerItemClickListener
import com.h4pay.store.recyclerAdapter.orderRecycler
import com.h4pay.store.recyclerAdapter.productRecycler
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class stockManagerActivity(): AppCompatActivity() {

    private val TAG = "stockManagerActivity"
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var productNameView:TextView
    private lateinit var productPriceView :TextView
    private lateinit var discountButton :Button
    private lateinit var inStockView:Switch
    private lateinit var mkProduct:Button
    private lateinit var rmProduct:Button
    private lateinit var selectedImage:ImageView
    private lateinit var submitPD:Button
    private lateinit var cacheFile:File
    private var selectedProduct:Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stockmanager)
        loadUI()
        loadList() //RecyclerView Init
        loadProduct(0)
    }

    fun loadUI(){
        productNameView = findViewById(R.id.productName)
        productPriceView = findViewById(R.id.productPrice)
        discountButton = findViewById(R.id.discountApply)
        inStockView = findViewById(R.id.soldout)
        recyclerView = findViewById(R.id.productRecyclerView)
        mkProduct = findViewById(R.id.mkProduct)
        rmProduct = findViewById(R.id.rmProduct)

        mkProduct.setOnClickListener{
            val dialogView = layoutInflater.inflate(R.layout.dialog_mkproduct, null)
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogView)
                .show()

            val pdName = dialogView.findViewById<EditText>(R.id.pdName)
            val pdPrice = dialogView.findViewById<EditText>(R.id.pdPrice)
            val pdDesc = dialogView.findViewById<EditText>(R.id.pdDesc)
            val imgPicker = dialogView.findViewById<Button>(R.id.imgPicker)
            submitPD = dialogView.findViewById<Button>(R.id.submitPD)
            selectedImage = dialogView.findViewById(R.id.selectedImage)

            imgPicker.setOnClickListener {
                val intent = Intent()
                intent.setType("image/*")
                intent.setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(intent, 1)
            }


            submitPD.setOnClickListener {
                val productName = pdName.text.toString()
                val productPrice = pdPrice.text.toString()
                val productDesc = pdDesc.text.toString()
                uploadImage().upload(productName, productPrice, productDesc, cacheFile)
            }
        }

        rmProduct.setOnClickListener {
            if (selectedProduct == null){
                Toast.makeText(this@stockManagerActivity, "삭제할 제품을 선택해주세요!", Toast.LENGTH_SHORT).show()
            }
            else {
                rmProductClass().rm(selectedProduct!!)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 1 || resultCode != RESULT_OK){
            return;
        }
        val dataUri = data?.data
        selectedImage.setImageURI(dataUri)

        try{
            val ins = contentResolver.openInputStream(dataUri!!)
            val image = BitmapFactory.decodeStream(ins)
            selectedImage.setImageBitmap(image)
            ins!!.close()
            val date = SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(Date())
            File.createTempFile("thumbnail_${date}.png", null, this.cacheDir)
            cacheFile = File(this.cacheDir, "thumbnail_${date}.png")
            val out = FileOutputStream(cacheFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, out)

        } catch (e:IOException){
            e.printStackTrace()
        }
        submitPD.isEnabled = true

    }


    fun loadProduct(position: Int){
        inStockView.setOnCheckedChangeListener(null)
        val item = prodList.getJSONObject(position)
        val soldout = item.getBoolean("soldout")
        productNameView.text = item.getString("productName")
        productPriceView.text = item.getString("price") + " 원"
        discountButton.setOnClickListener {
            //TODO: 할인 Dialog Display
        }
        inStockView.textOn = "재고 있음"
        inStockView.textOff = "품절"
        inStockView.isChecked = soldout


        inStockView.setOnCheckedChangeListener { _, isChecked ->
            prodList = getProdList().execute().get()
            Log.d(TAG, isChecked.toString())
            if (isChecked && !soldout){
                yesNoDialog(this@stockManagerActivity,"확인", "정말로 품절처리 하시겠습니까?", {
                    Log.d(TAG, "soldoutTry")
                    val state = JSONObject()
                    state.accumulate("target", position)
                    state.accumulate("productName", item.getString("productName"))
                    state.accumulate("price", item.getInt("price"))
                    state.accumulate("desc", item.getString("desc"))
                    state.accumulate("img", item.getString("img"))
                    state.accumulate("soldout", isChecked)
                    val res = productStateChanger(state).execute().get()
                    if (res.getBoolean("modifySuccess")){
                        Toast.makeText(this@stockManagerActivity, "상품 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                        startActivity(intent)
                    }
                },{
                    inStockView.isChecked = false
                })
            }else if(!isChecked && soldout){
                yesNoDialog(this@stockManagerActivity, "확인", "정말로 재고 있음 상태로 만드시겠습니까?", {
                    val state = JSONObject()
                    state.accumulate("target", position)
                    state.accumulate("productName", item.getString("productName"))
                    state.accumulate("price", item.getInt("price"))
                    state.accumulate("desc", item.getString("desc"))
                    state.accumulate("img", item.getString("img"))
                    state.accumulate("soldout", isChecked)
                    if (productStateChanger(state).execute().get().getBoolean("modifySuccess")){
                        Toast.makeText(this@stockManagerActivity, "상품 수정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                        startActivity(intent)
                    }
                },{
                    inStockView.isChecked = true
                })
            }
        }

        discountButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_discount, null)
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogView)
                .show()

            val editText = dialogView.findViewById<EditText>(R.id.discountValue)

            dialogView.findViewById<RadioGroup>(R.id.radioGroup).setOnCheckedChangeListener{ radioGroup: RadioGroup, i: Int ->
                if (i == R.id.wonCheck){
                    editText.hint = "₩"
                }else if(i == R.id.percentCheck){
                    editText.hint = "%"
                    editText.setEms(3)
                }
            }

            dialogView.findViewById<Button>(R.id.dcApply).setOnClickListener {

                if (editText.text.toString() != ""){
                    if (dialogView.findViewById<RadioButton>(R.id.wonCheck).isChecked){ //원화
                        val state = JSONObject()
                        state.accumulate("target", position)
                        state.accumulate("productName", item.getString("productName"))
                        state.accumulate("price", editText.text.toString().toInt())
                        state.accumulate("desc", item.getString("desc"))
                        state.accumulate("img", item.getString("img"))
                        state.accumulate("soldout", item.getBoolean("soldout"))
                        val res = productStateChanger(state).execute().get() //result
                        Toast.makeText(this, res.getString("message"), Toast.LENGTH_SHORT).show()
                        productPriceView.text = editText.text.toString() + " 원"
                    }
                    else{ //할인율
                        if (editText.text.toString().length <= 3){
                            val state = JSONObject()
                            state.accumulate("target", position)
                            state.accumulate("productName", item.getString("productName"))
                            state.accumulate("price", item.getInt("price") - (item.getInt("price") * editText.text.toString().toInt() / 100))
                            state.accumulate("desc", item.getString("desc"))
                            state.accumulate("img", item.getString("img"))
                            state.accumulate("soldout", item.getBoolean("soldout"))
                            val res = productStateChanger(state).execute().get()
                            Toast.makeText(this, res.getString("message"), Toast.LENGTH_SHORT).show()
                            productPriceView.text = (item.getInt("price") - (item.getInt("price") * editText.text.toString().toInt() / 100)).toString()
                        } else {

                        }
                    }
                }
            }


        }
    }

    fun loadList(){
        //Load Data
        prodList = getProdList().execute().get()

        //RecyclerView Init
        viewManager = LinearLayoutManager(this)
        viewAdapter = productRecycler(this, prodList)
        recyclerView.apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            adapter = viewAdapter
            layoutManager = viewManager

            addOnItemTouchListener(
                RecyclerItemClickListener(applicationContext, this,
                    object : RecyclerItemClickListener.OnItemClickListener {
                        @SuppressLint("SetTextI18n")
                        override fun onItemClick(view: View, position: Int) {
                            loadProduct(position)
                            selectedProduct = position
                        }
                    }
                )
            )
        }
    }
}