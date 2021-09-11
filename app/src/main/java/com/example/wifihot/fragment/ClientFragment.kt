package com.example.wifihot.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.wifihot.BleServer
import com.example.wifihot.BleServer.socket
import com.example.wifihot.Response
import com.example.wifihot.TcpCmd
import com.example.wifihot.client.ImageJpeg
import com.example.wifihot.databinding.FragmentClientBinding
import com.example.wifihot.databinding.FragmentMainBinding
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Dispatcher
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException
import kotlin.experimental.inv

class ClientFragment:Fragment() {
    lateinit var binding: FragmentClientBinding
    lateinit var wifiManager: WifiManager
    var wifiState = 0
    private var pool: ByteArray? = null
    lateinit var imageJpeg: ImageJpeg
    private var fileDataChannel = Channel<ByteArray>(Channel.CONFLATED)
    private val fileChannel = Channel<Int>(Channel.CONFLATED)


    private fun isWifiConnected(): Boolean {
        val connectivityManager =
            requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiNetworkInfo!!.isConnected
    }

    private fun getConnectWifiSsid(): String? {
        val wifiInfo = wifiManager!!.connectionInfo
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
        return ((paramInt.and(255)).toString() + "." + (paramInt.shr(8).and(255)) + "." + (paramInt.shr(16).and(255)) + "."
                + (paramInt.shr(24).and(255)))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wifiManager =requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager


        val gate=intToIp(wifiManager.dhcpInfo.gateway)
        if (gate != null) {
            BleServer.dataScope.launch {
                try {
                    socket = Socket(gate, 9999)
                    BleServer.startRead()
                } catch (e: UnknownHostException) {
                    println("请检查端口号是否为服务器IP")
                    e.printStackTrace()
                } catch (e: IOException) {
                    println("服务器未开启")
                    e.printStackTrace()
                }
            }

        }

        requireContext().registerReceiver(wifiBroadcast,makeGattUpdateIntentFilter())
        binding= FragmentClientBinding.inflate(inflater,container,false)




        BleServer.receive=object :BleServer.Receive{
            override fun tcpReceive(byteArray: ByteArray) {
                byteArray.apply {
                    pool = add(pool, this)
                }
                pool?.apply {
                    pool = handleDataPool(pool)
                }
            }

        }
        var time=System.currentTimeMillis()

        var count=0;

        BleServer.dataScope.launch {
            delay(1000)
            time=System.currentTimeMillis()
            while(true){
                val pic=GetPic()
                withContext(Dispatchers.Main){
                    count++
                    if(count>=10){
                        val x=(System.currentTimeMillis()-time).toFloat()/1000f
                        binding.fps.text=(10f/x).toInt().toString()+" fps"
                        time=System.currentTimeMillis()
                        count=0
                    }
                    binding.img.setImageBitmap(pic)
                }
                delay(20)
            }
        }


        return binding.root
    }


    val lock= Mutex()

    private suspend  fun GetPic():Bitmap?{
        lock.withLock {
            val dum=withTimeoutOrNull(1000){
                withTimeoutOrNull(200){
                    BleServer.send(TcpCmd.readFileStart())
                    fileChannel.receive()
                }
                while (imageJpeg.index<imageJpeg.size){
                    BleServer.send(TcpCmd.readFileData(imageJpeg.index))
                    withTimeoutOrNull(200){
                        imageJpeg.add(fileDataChannel.receive())
                    }
                }
            }
            if(dum==null){
                return null
            }
            return BitmapFactory.decodeStream(ByteArrayInputStream(imageJpeg.content))
        }
    }

    private fun handleDataPool(bytes: ByteArray?): ByteArray? {
        val bytesLeft: ByteArray? = bytes

        if (bytes == null || bytes.size < 8) {
            return bytes
        }
        loop@ for (i in 0 until bytes.size - 7) {
            if (bytes[i] != 0xA5.toByte() || bytes[i + 1] != bytes[i + 2].inv()) {
                continue@loop
            }

            // need content length
            val len = toUInt(bytes.copyOfRange(i + 5, i + 7))
            if (i + 8 + len > bytes.size) {
                continue@loop
            }

            val temp: ByteArray = bytes.copyOfRange(i, i + 8 + len)
            if (temp.last() == CRCUtils.calCRC8(temp)) {

                val bleResponse = Response(temp)
                onResponseReceived(bleResponse)
                val tempBytes: ByteArray? =
                    if (i + 8 + len == bytes.size) null else bytes.copyOfRange(
                        i + 8 + len,
                        bytes.size
                    )

                return handleDataPool(tempBytes)
            }
        }

        return bytesLeft
    }


    private fun onResponseReceived(response: Response) {

        when (response.cmd) {
            TcpCmd.CMD_READ_FILE_START->{
                val fileSize=toUInt(response.content)
                imageJpeg= ImageJpeg(fileSize)
                BleServer.dataScope.launch {
                    fileChannel.send(fileSize)
                }
            }
            TcpCmd.CMD_READ_FILE_DATA->{
                BleServer.dataScope.launch {
                    fileDataChannel.send(response.content)
                }
            }
        }
    }

}