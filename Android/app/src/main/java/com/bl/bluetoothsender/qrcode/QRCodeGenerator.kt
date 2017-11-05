package com.bt.buetoothfiletransfer.qrcode

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import android.view.WindowManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException

/**
 * Created by Ganesh Kanna on 19-08-2017.
 */

object QRCodeGenerator {
    fun getQRCodeBitmap(context: Context, qrInputText: String): Bitmap? {

        //Find screen size
        Log.i("TAG", "QR code " + qrInputText)
        val manager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        val point = Point()
        display.getSize(point)
        val width = point.x
        val height = point.y
        var smallerDimension = if (width < height) width else height
        smallerDimension = smallerDimension * 3 / 4

        //Encode with a QR Code image
        val qrCodeEncoder = QRCodeEncoder(qrInputText,
                null,
                Contents.Type.TEXT,
                BarcodeFormat.QR_CODE.toString(),
                smallerDimension)
        try {
            return qrCodeEncoder.encodeAsBitmap()
        } catch (e: WriterException) {
            e.printStackTrace()
        }

        return null
    }
}
