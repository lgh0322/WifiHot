package com.example.wifihot.fragment

import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.BleServer
import com.example.wifihot.BleServer.dataScope
import com.example.wifihot.BleServer.socket
import com.example.wifihot.Response
import com.example.wifihot.TcpCmd
import com.example.wifihot.databinding.FragmentMainBinding
import com.example.wifihot.databinding.FragmentServerBinding
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Runnable
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.inv

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


    lateinit var wifiManager: WifiManager
    private val PORT = 9999

    var pool:ByteArray?=null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager


        BleServer.dataScope.launch {
            val server = ServerSocket(PORT)
            socket = server.accept()
            BleServer.startRead()
        }

        binding = FragmentServerBinding.inflate(inflater, container, false)

        BleServer.receive = object : BleServer.Receive {
            override fun tcpReceive(byteArray: ByteArray) {
                pool=add(pool,byteArray)
                pool=poccessLinkData()
            }

        }


        return binding.root
    }





    fun poccessLinkData():ByteArray? {
        var bytes =pool
        while (true){
            if (bytes == null || bytes.size < 11) {
                break
            }
            var con=false

            loop@ for (i in 0 until bytes!!.size - 10) {
                if (bytes!![i] != 0xA5.toByte() || bytes[i + 1] != bytes[i + 2].inv()) {
                    continue@loop
                }

                // need content length
                val len = toUInt(bytes.copyOfRange(i + 6, i + 10))
                if(len<0){
                    continue@loop
                }
                if (i + 11 + len > bytes.size) {
                    continue@loop
                }

                val temp: ByteArray = bytes.copyOfRange(i, i + 11 + len)
                if (temp.last() == CRCUtils.calCRC8(temp)) {
                   onResponseReceived(Response(temp))
                    val tempBytes: ByteArray? =
                        if (i + 11 + len == bytes.size) null else bytes.copyOfRange(
                            i + 11 + len,
                            bytes.size
                        )

                    bytes=tempBytes
                    con=true
                    break@loop
                }
            }
            if(!con){
                return bytes
            }else{
                con=false
            }

        }
        return null
    }


    val loc= Mutex()

    fun onResponseReceived(x:Response){
        when(x.cmd){
            TcpCmd.CMD_READ_FILE_DATA->{
                MainScope().launch {
                    loc.withLock {
                        val fg = BitmapFactory.decodeStream(ByteArrayInputStream(x.content.clone()))
                        if(fg!=null){
                            binding.img.setImageBitmap(fg)
                        }

                    }

                    dataScope.launch {
                        BleServer.send("fuck".toByteArray())
                    }

                }
            }
            TcpCmd.CMD_READ_FILE_START->{

            }
        }
    }



}