package com.example.wifihot.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.*
import com.example.wifihot.ClientHeart.mySocket
import com.example.wifihot.databinding.FragmentClientBinding
import com.example.wifihot.tcp.TcpCmd
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException

class ClientFragment : Fragment() {
    lateinit var binding: FragmentClientBinding
    lateinit var wifiManager: WifiManager
    var wifiState = 0
    private var pool: ByteArray? = null
    lateinit var imageJpeg: ImageJpeg
    private val fileChannel = Channel<Int>(Channel.CONFLATED)
    lateinit var sendIndex: IntArray
    private var fileDataChannel = Channel<ByteArray>(Channel.CONFLATED)

    var clientId = 0

    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiNetworkInfo!!.isConnected
    }

    private fun getConnectWifiSsid(): String? {
        val wifiInfo = wifiManager.connectionInfo
        Log.d("wifiInfo", wifiInfo.toString())
        Log.d("SSID", wifiInfo.ssid)
        return wifiInfo.ssid
    }

    private val wifiBroadcast: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // TODO Auto-generated method stub
            if (intent.action == WifiManager.RSSI_CHANGED_ACTION) {
                //signal strength changed
            } else if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) { //wifi打开与否
                val wifistate = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_DISABLED
                )
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    println("系统关闭wifi")

                    wifiState = 0
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    println("系统开启wifi")
                    wifiState = if (isWifiConnected()) {
                        if (getConnectWifiSsid() == "\"wifisocket\"") {
                            3
                        } else {
                            4
                        }
                    } else {
                        1
                    }
                }
            } else if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION) { //wifi连接上与否
                println("网络状态改变")
                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                if (info!!.state == NetworkInfo.State.DISCONNECTED) {
                    println("wifi网络连接断开")
                    wifiState = 2
                } else if (info.state == NetworkInfo.State.CONNECTED) {
                    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo = wifiManager.connectionInfo

                    //获取当前wifi名称
                    println("连接到网络 " + wifiInfo.ssid)
                    wifiState = if (wifiInfo.ssid == "\"wifisocket\"") {
                        3
                    } else {
                        4
                    }
                }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val filter = IntentFilter()
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION)
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        return filter
    }


    private fun intToIp(paramInt: Int): String? {
        return ((paramInt.and(255)).toString() + "." + (paramInt.shr(8)
            .and(255)) + "." + (paramInt.shr(16).and(255)) + "."
                + (paramInt.shr(24).and(255)))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wifiManager =
            MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val gate = intToIp(wifiManager.dhcpInfo.gateway)
        if (gate != null) {
            ClientHeart.dataScope.launch {
                try {
                    mySocket = MySocket(Socket(NetInfo.server,NetInfo.port))
                    ClientHeart.startRead()
                } catch (e: UnknownHostException) {
                    println("请检查端口号是否为服务器IP")
                    e.printStackTrace()
                } catch (e: IOException) {
                    println("服务器未开启")
                    e.printStackTrace()
                }
            }

        }

        requireContext().registerReceiver(wifiBroadcast, makeGattUpdateIntentFilter())
        binding = FragmentClientBinding.inflate(inflater, container, false)




        ClientHeart.receive = object : ClientHeart.Receive {
            override fun onResponseReceived(response: Response, mySocket: MySocket) {
                when (response.cmd) {
                    TcpCmd.CMD_READ_FILE_START -> {
                        val fileSize = toUInt(response.content)
                        imageJpeg = ImageJpeg()
                        clientId = response.id
                        ClientHeart.send(TcpCmd.readFileData(0, clientId))
                    }
                    TcpCmd.CMD_READ_FILE_DATA -> {
                        ClientHeart.dataScope.launch {
                            imageJpeg.add(response.content)
                            fileDataChannel.send(byteArrayOf(0))
                        }
                    }
                }
            }

        }
        var time = 0L

        var count = 0;

        val fuckx= Mutex()
        ClientHeart.dataScope.launch {
            try {
                delay(1000)
                time = System.currentTimeMillis()
                while (true) {
                    val pic = GetPic()
                    if (pic == null) {
                        continue
                    }
                    val fx = pic.clone()
                    withContext(Dispatchers.Main) {
                        count++
                        if (count >= 10) {
                            val x = (System.currentTimeMillis() - time).toFloat() / 1000f
                            binding.fps.text = (10f / x).toInt().toString() + " fps"
                            time = System.currentTimeMillis()
                            count = 0
                        }
                        fuckx.withLock {
                            val fg = BitmapFactory.decodeStream(ByteArrayInputStream(fx))
                            binding.img.setImageBitmap(fg)
                        }

                    }
                }
            } catch (e: Exception) {

            }

        }


        return binding.root
    }


    val lock = Mutex()
    private suspend fun GetPic(): ByteArray? {
        try {
            lock.withLock {

                val c = withTimeoutOrNull(2000) {
                    ClientHeart.send(TcpCmd.readFileStart())
                    fileDataChannel.receive()
                }
                if (c == null) {
                    return null
                }


                return imageJpeg.content
            }

        } catch (e: Exception) {
            return null
        }

    }


}