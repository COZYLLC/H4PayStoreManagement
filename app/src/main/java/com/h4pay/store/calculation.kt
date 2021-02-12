package com.h4pay.store

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class calculation {
    private val TAG = "calculation"
    fun calculate(context: Context, jsonArray: JSONArray){
        val f = NumberFormat.getInstance()
        f.isGroupingUsed = false
        val wb = HSSFWorkbook() //Excel Object
        val sheet1 = wb.createSheet("H4Pay 구매내역")
        val row = sheet1.createRow(0)
        row.createCell(1).setCellValue("ID")
        row.createCell(2).setCellValue("주문번호")
        row.createCell(3).setCellValue("결제키")
        row.createCell(4).setCellValue("주문일")
        row.createCell(5).setCellValue("주문품목")
        row.createCell(6).setCellValue("만료기한")
        row.createCell(7).setCellValue("결제금액")
        row.createCell(8).setCellValue("교환여부")
        for (x in (0 until jsonArray.length())){
            val jsonObject = jsonArray.getJSONObject(x)
            val row = sheet1.createRow(x+1)
            row.createCell(0).setCellValue("${x+1}")
            row.createCell(1).setCellValue(jsonObject.getString("uid"))
            row.createCell(2).setCellValue(f.format(jsonObject.getDouble("orderid")))
            row.createCell(3).setCellValue(jsonObject.getString("paymentkey"))
            row.createCell(4).setCellValue(jsonObject.getString("date"))
            row.createCell(5).setCellValue(jsonObject.getJSONArray("item").toString())
            row.createCell(6).setCellValue(jsonObject.getString("expire"))
            row.createCell(7).setCellValue(jsonObject.getString("amount"))
            row.createCell(8).setCellValue(if (jsonObject.getBoolean("exchanged")){"O"} else {"X"})
        }
        val date = Calendar.getInstance().time
        val dateString: String = SimpleDateFormat("yyyy-MM-dd").format(date)
        val filename = "[${dateString}]H4Pay 구매내역 정산.xls"
        val dir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val xls = File(dir, filename)
        try{
            val os = FileOutputStream(xls)
            wb.write(os)
            Toast.makeText(context, "정산 파일 생성에 성공하였습니다!", Toast.LENGTH_SHORT).show()
        }catch (e:Exception){
            Log.e(TAG, e.toString())
            Toast.makeText(context, "정산 파일 생성에 실패했습니다..", Toast.LENGTH_SHORT).show()
        }
    }
}