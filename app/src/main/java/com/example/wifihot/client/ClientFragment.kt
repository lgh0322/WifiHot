package com.example.wifihot.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaFormat
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.*
import com.example.wifihot.ClientHeart.mySocket
import com.example.wifihot.databinding.FragmentClientBinding
import com.example.wifihot.tcp.TcpCmd
import com.example.wifihot.utiles.toUInt
import com.vaca.fuckh264.record.VideoRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException
import java.nio.ByteBuffer

class ClientFragment : Fragment() {
    lateinit var binding: FragmentClientBinding
    lateinit var wifiManager: WifiManager
    var wifiState = 0
    private var pool: ByteArray? = null
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

    var countgg=0
    var time=0L

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
                    mySocket = MySocket(Socket(NetInfo.server, NetInfo.port))
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

                        setupDecoder(binding.ga.holder.surface, MediaFormat.MIMETYPE_VIDEO_HEVC, 1080, 1920,response.content)
                        clientId = response.id
                        ClientHeart.send(TcpCmd.readFileData(0, clientId))
                    }
                    TcpCmd.CMD_READ_FILE_DATA -> {
                        ClientHeart.dataScope.launch {
                            val fx = response.content.clone()
                            val f1=fx.copyOfRange(0,8)
                            val timestamp=Long2BytesUtils.byteArrayToLong(f1)
                            Log.e("fuckfcuasdfs",timestamp.toString())
                            val f2=fx.copyOfRange(8,fx.size)
                            offerDecoder(f2,f2.size,timestamp)
                            withContext(Dispatchers.Main) {
                        countgg++
                        if (countgg >= 10) {
                            val x = (System.currentTimeMillis() - time).toFloat() / 1000f
                            binding.fps.text = (10f / x).toInt().toString() + " fps"
                            time = System.currentTimeMillis()
                            countgg = 0
                        }


                    }

                        }
                    }
                }
            }

        }
        var time = 0L

        var count = 0;

//        ClientHeart.dataScope.launch {
//            try {
//                delay(1000)
//                time = System.currentTimeMillis()
//                while (true) {
//                    val pic = GetPic()
//                    if (pic == null) {
//                        continue
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        count++
//                        if (count >= 10) {
//                            val x = (System.currentTimeMillis() - time).toFloat() / 1000f
//                            binding.fps.text = (10f / x).toInt().toString() + " fps"
//                            time = System.currentTimeMillis()
//                            count = 0
//                        }
//
//
//                    }
//                }
//            } catch (e: Exception) {
//
//            }
//
//        }


        return binding.root
    }
    private  val TAG = "StudyCamera"
    private var mMediaDecoder: MediaCodec? = null

    private fun setupDecoder(surface: Surface?, mime: String, width: Int, height: Int,vpsspspps:ByteArray): Boolean {
        Log.d(TAG, "setupDecoder surface:$surface mime:$mime w:$width h:$height")
        val format = MediaFormat.createVideoFormat(mime, width, height)
        mMediaDecoder = MediaCodec.createDecoderByType(mime)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1080 * 1920)
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, 1920)
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, 1080)
        format.setByteBuffer("csd-0", ByteBuffer.wrap(vpsspspps))
        mMediaDecoder!!.configure(format, surface, null, 0)
        mMediaDecoder!!.start()
        return true
    }

    private fun offerDecoder(input: ByteArray, length: Int, time: Long) {
        try {
            val inputBuffers = mMediaDecoder!!.inputBuffers
            val inputBufferIndex = mMediaDecoder!!.dequeueInputBuffer(-1)
            if (inputBufferIndex >= 0) {
                val inputBuffer = inputBuffers[inputBufferIndex]
                val timestamp = time
                Log.d(TAG, "offerDecoder timestamp: $timestamp inputSize: $length bytes")
                inputBuffer.clear()
                inputBuffer.put(input, 0, length)
                mMediaDecoder!!.queueInputBuffer(inputBufferIndex, 0, length, timestamp, 0)
            }
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            while (outputBufferIndex >= 0) {
                Log.d(TAG, "offerDecoder OutputBufSize:" + bufferInfo.size + " bytes written")

                //If a valid surface was specified when configuring the codec,
                //passing true renders this output buffer to the surface.
                mMediaDecoder!!.releaseOutputBuffer(outputBufferIndex, true)
                outputBufferIndex = mMediaDecoder!!.dequeueOutputBuffer(bufferInfo, 0)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }





}