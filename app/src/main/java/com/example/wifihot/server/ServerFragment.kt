package com.example.wifihot.server

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
import androidx.collection.arrayMapOf
import androidx.fragment.app.Fragment
import com.example.wifihot.MainApplication
import com.example.wifihot.MySocket

import com.example.wifihot.Response
import com.example.wifihot.ServerHeart
import com.example.wifihot.ServerHeart.dataScope
import com.example.wifihot.ServerHeart.server
import com.example.wifihot.tcp.TcpCmd
import com.example.wifihot.databinding.FragmentServerBinding
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.lang.Runnable
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList
import kotlin.experimental.inv

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


    lateinit var wifiManager: WifiManager
    private val PORT = 9999




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager = MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        ServerHeart.dataScope.launch {
            server = ServerSocket(PORT)
            ServerHeart.dataScope.launch {
                ServerHeart.startAccept()
            }

        }

        binding = FragmentServerBinding.inflate(inflater, container, false)



        dataScope.launch {
            delay(10000)
            val b=ByteArray(2000000){
                it.toByte()
            }
            while (true){
                ServerHeart.send(b)
            }
        }

        return binding.root
    }




    override fun onDestroy() {
        server.close()
        super.onDestroy()
    }


}