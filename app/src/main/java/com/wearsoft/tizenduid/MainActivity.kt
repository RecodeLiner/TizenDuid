package com.wearsoft.tizenduid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cgutman.adblib.AdbCrypto
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wearsoft.tizenduid.databinding.ActivityMainBinding
import java.lang.reflect.Method
import java.net.ConnectException
import java.net.InetAddress
import java.net.NoRouteToHostException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.concurrent.timerTask


private const val PREFS_NAME = "preferences"
private const val PREF_IP = "IP"
private const val DefaultIPValue = "192.168.0."

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bluetoothPermission = false
    private var wearIp: String = ""
    private var phoneIp: String = ""
    private var phoneNetwork = ""
    private var appActive = false
    private var tizenDevice = ""
    private var sdbPort = 26101
    private lateinit var watchModelTextView: TextView
    private lateinit var watchProductTextView: TextView
    private lateinit var watchDuidTextView: TextView
    private lateinit var helpTextView: TextView
    private lateinit var phoneWiFiIcon: ImageView
    private lateinit var watchConnectionIcon: ImageView
    private lateinit var watchWiFiIcon: ImageView
    private lateinit var watchSdbIcon: ImageView
    private lateinit var watchImage: ImageView
    private lateinit var copyButton: Button
    private lateinit var dsdb: Dsdb
    private var gotReply = false
    private var watchSuspend = false
    private var adbDebugPermission = false

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            bluetoothPermission = true
        }else{
            helpTextView.text = getString(R.string.btpermission)
        }
        checkMobileWiFiConnection()
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if (it.value) {
                    bluetoothPermission = true
                } else {
                    helpTextView.text = getString(R.string.btpermission)
                }
            }
            checkMobileWiFiConnection()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        watchModelTextView = binding.model
        watchProductTextView = binding.product
        watchDuidTextView = binding.duid
        helpTextView = binding.helpText
        phoneWiFiIcon = binding.phoneWiFi
        watchConnectionIcon = binding.btConnection
        watchWiFiIcon = binding.watchWiFi
        watchSdbIcon = binding.adbEnabled
        watchImage = binding.watch
        copyButton = binding.button

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMultiplePermissions.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT))
            }
            else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }
        } else {
            bluetoothPermission = true
            checkMobileWiFiConnection()
        }

    }

    private fun checkMobileWiFiConnection() {
        val wifiManager: WifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager.isWifiEnabled) {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network : Network) {
                }

                override fun onLost(network : Network) {
                }

                override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                }

                override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
                    val netAddressNumber: Int = linkProperties.linkAddresses.size
                    if (netAddressNumber > 1) {
                        val ipAdr: InetAddress = linkProperties.linkAddresses[1].address
                        phoneIp = ipAdr.toString().replace("/","")
                        runOnUiThread {
                            phoneWiFiIcon.setImageResource(R.drawable.phonewifion)
                        }
                        findTizenDevices()
                    } else {
                        if (appActive) {
                            helpTextView.text = getString(R.string.phonewifi)
                        }
                    }
                }
            })
        } else {
            helpTextView.text = getString(R.string.phonewifioff)
        }
    }

    @SuppressLint("MissingPermission")
    private fun findTizenDevices() {

        if (bluetoothPermission) {
            val btManager = baseContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val pairedDevices = btManager.adapter.bondedDevices

            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    val deviceName = device.name
                    if (isConnected(device) && deviceName.contains("Galaxy") && !deviceName.contains("4 ")) {
                        tizenDevice = deviceName
                    }
                }
                if (tizenDevice != "") {
                    runOnUiThread {
                        watchProductTextView.text = String.format(getString(R.string.product), tizenDevice)
                        watchConnectionIcon.setImageResource(R.drawable.watchphoneon)
                        showDialog()
                    }
                }
            }
        }
    }

    private fun showDialog(){
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(getString(R.string.watchIp))

        val input = EditText(this)
        input.setText(loadLastIp())
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            // Here you get get input text from the Edittext
            wearIp = input.text.toString()
            if (wearIp.contains(":")) {
                wearIp = wearIp.substring(0, wearIp.indexOf(":"))
            }
            helpTextView.text = getString(R.string.searching)
            getAdbKeys()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }


        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.decorView?.setBackgroundResource(R.drawable.round_corner)
        dialog.show()
    }

    private fun isValidIPAddress(ip: String?): Boolean {

        val zeroTo255 = ("(\\d{1,2}|(0|1)\\"
                + "d{2}|2[0-4]\\d|25[0-5])")

        val regex = (zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255 + "\\."
                + zeroTo255)

        val p: Pattern = Pattern.compile(regex)

        if (ip == null) {
            return false
        }

        val m: Matcher = p.matcher(ip)

        return m.matches()
    }

    private fun getAdbKeys() {
        val wearNetwork = wearIp.substring(0, wearIp.lastIndexOf("."))
        if (isValidIPAddress(wearIp)) {

            val crypto: AdbCrypto? = AdbUtils.readCryptoConfig(filesDir)
            if (crypto == null) {
                runOnUiThread {
                    val cryptos: AdbCrypto? = AdbUtils.writeNewCryptoConfig(filesDir)
                    if (cryptos == null) {
                        helpTextView.text = getString(R.string.rsaerror)
                        return@runOnUiThread
                    } else {
                        helpTextView.text = getString(R.string.rsaok)
                        sendAdbHello()
                    }
                }
            } else {
                sendAdbHello()
            }
        } else {
            Toast.makeText(applicationContext, getString(R.string.iperror), Toast.LENGTH_SHORT).show()
            doSomethingAfterToast()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun sendAdbHello() {

        Thread {
            dsdb = Dsdb.create(filesDir, wearIp, sdbPort)
            try {
                Timer().schedule(timerTask {
                    if (gotReply) {
                        runOnUiThread {
                            watchWiFiIcon.setImageResource(R.drawable.watchwifion)
                        }
                    } else {
                        runOnUiThread {
                            if (!watchSuspend) {
                                helpTextView.text = getString(R.string.notfound)
                            }
                        }
                    }
                }, 10000)
                try {
                    val response = dsdb.shell("/opt/etc/duid-gadget anystring")
                    val m = dsdb.connectionString()
                    if (m.isNotEmpty()) {
                        runOnUiThread {
                            watchModelTextView.text =
                                String.format(getString(R.string.model), m.toString())
                        }
                        if (m.contains("SM-R800") || m.contains("SM-R805") || m.contains("SM-R810") || m.contains(
                                "SM-R815"
                            )
                        ) {
                            runOnUiThread {
                                watchImage.setImageResource(R.drawable.gw)
                            }
                        }
                        if (m.contains("SM-R855") || m.contains("SM-845F") || m.contains("SM-R845") || m.contains(
                                "SM-R850"
                            ) || m.contains("SM-R840")
                        ) {
                            runOnUiThread {
                                watchImage.setImageResource(R.drawable.gw3)
                            }
                        }
                        if (m.contains("SM-R500")) {
                            runOnUiThread {
                                watchImage.setImageResource(R.drawable.gwactive)
                            }
                        }
                        if (m.contains("SM-R820") || m.contains("SM-R825") || m.contains("SM-R830") || m.contains(
                                "SM-R835"
                            )
                        ) {
                            runOnUiThread {
                                watchImage.setImageResource(R.drawable.gwactive2)
                            }
                        }
                        if (response.contains("2.0#")) {
                            gotReply = true
                            saveLastIpAndLogin()
                            adbDebugPermission = true
                            watchSuspend = false
                            runOnUiThread {
                                helpTextView.text = ""
                                watchWiFiIcon.setImageResource(R.drawable.watchwifion)
                                watchSdbIcon.setImageResource(R.drawable.watchadbon)
                                watchDuidTextView.text = "DUID: $response"
                                copyButton.setOnClickListener {
                                    val clipboard: ClipboardManager = getSystemService(
                                        CLIPBOARD_SERVICE
                                    ) as ClipboardManager
                                    val clip = ClipData.newPlainText("DUID", response)
                                    clipboard.setPrimaryClip(clip)
                                    helpTextView.text = getString(R.string.copied)
                                }
                            }
                        }
                    }
                } catch (e: ConnectException) {
                    runOnUiThread {
                        helpTextView.text = getString(R.string.conerror)
                    }
                }
            } catch (e: NoRouteToHostException) {
                runOnUiThread {
                    watchSuspend = true
                    Toast.makeText(
                        applicationContext,
                        "Часы с IP $wearIp недоступны",
                        Toast.LENGTH_SHORT
                    ).show()
                    doSomethingAfterToast()
                }
            }
        }.start()
    }

    private fun doSomethingAfterToast() {
        Handler(mainLooper).postDelayed({ showDialog() }, 3000)
    }

    private fun saveLastIpAndLogin() {
        val settings = getSharedPreferences(PREFS_NAME,MODE_PRIVATE)
        val editor = settings.edit()

        editor.putString(PREF_IP, wearIp)
        editor.apply()
    }

    private fun loadLastIp(): String {
        val settings = getSharedPreferences(PREFS_NAME,MODE_PRIVATE)

        return settings.getString(PREF_IP, DefaultIPValue) ?: "192.168.0."
    }

    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    @Override
    override fun onStart() {
        super.onStart()
        appActive = true
    }

    @Override
    override fun onStop() {
        super.onStop()
        appActive = false
    }

}