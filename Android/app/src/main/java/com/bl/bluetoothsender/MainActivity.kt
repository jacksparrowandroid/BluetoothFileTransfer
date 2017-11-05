package com.bl.bluetoothsender

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bt.buetoothfiletransfer.qrcode.QRCodeGenerator
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        protected val REQUEST_LOCATION_PERMISSION = 102
        protected val REQUEST_STORAGE_PERMISSION = 101
        private val REQUEST_ENABLE_BT = 3
        val PICK_IMAGE_REQUEST = 200;
    }

    var outputFileUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        checkLocationPermission()
        selectImage.setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(p0: View?) {
                        pickImage()
                    }
                })
    }

    private var mBluetoothAdapter: BluetoothAdapter? = null
    public override fun onStart() {
        super.onStart()
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        mBluetoothAdapter?.let {
            if (!it.isEnabled()) {
                it.enable()
                // Otherwise, setup the chat session
            }
        }

    }

    fun pickImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    private fun loadQRCode() {
        val macAddress = android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address")
        val image = QRCodeGenerator.getQRCodeBitmap(this, macAddress)
        qrcodeImage.setImageBitmap(image)
        infoText.visibility = View.VISIBLE
    }

    protected fun checkLocationPermission(): Boolean {
        // Check read/receive sms permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        REQUEST_LOCATION_PERMISSION)
                return false
            }
        }
        return true
    }

    protected fun checkStoragePermission(): Boolean {
        // Check read/receive sms permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                run {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onPermissionGranted(REQUEST_STORAGE_PERMISSION)
                    } else {
                        checkStoragePermission()
                    }
                }
                run {
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        onPermissionGranted(REQUEST_LOCATION_PERMISSION)
                    } else {
                        checkLocationPermission()
                    }
                }
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted(REQUEST_LOCATION_PERMISSION)
                } else {
                    checkLocationPermission()
                }
            }
        }
    }

    protected fun onPermissionGranted(requestCode: Int) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode === Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    outputFileUri = data?.data
                    loadQRCode()
                }

            }
        }
    }

    private val writer = Runnable {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val uuid = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66")
        try {
            val serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("BLTServer", uuid)
            Log.d("BL", "Server: Socket opened")
            val client = serverSocket.accept()
            Log.d("BL", "Server: connection done")
            Log.d("BL", "server: copying files " + outputFileUri?.getPath())
            val cr = getContentResolver()
            val inputStream = cr.openInputStream(outputFileUri)
            val stream = client.outputStream
            copyFile(inputStream, stream)
            serverSocket.close()
            //Show message on UIThread
            runOnUiThread(Runnable { Toast.makeText(this, "Transferred", Toast.LENGTH_LONG).show() })

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun copyFile(inputStream: InputStream?, out: OutputStream): Boolean {
        val buf = ByteArray(1024)
        inputStream?.let {
            var len: Int
            do {
                len = inputStream.read(buf)
                if (len > 0) {
                    out.write(buf, 0, len)
                }
            } while (len > 0)
        }
        return true
    }
}
