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



    private val mCameraId = "0"
    lateinit var mPreviewSize: Size
    private val PREVIEW_WIDTH = 1920
    private val PREVIEW_HEIGHT = 1080
    private var mCameraDevice: CameraDevice? = null
    lateinit var mHandler: Handler
    lateinit var mCaptureSession: CameraCaptureSession
    lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mHandlerThread: HandlerThread? = null
    lateinit var mImageReader: ImageReader
    private var pool0: ByteArray? = null
    private var pool1: ByteArray? = null

    val mtu = 2000

    var bitmap: Bitmap? = null

    val  serverSend= ConcurrentHashMap<Int,JpegSend>()


    lateinit var acceptJob: Job

    inner class JpegSend(var jpegArray: ByteArray){
        val jpegSize = jpegArray.size
        var jpegSeq = 0;
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager = MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        ServerHeart.dataScope.launch {
            server = ServerSocket(PORT)
            acceptJob = ServerHeart.dataScope.launch {
                ServerHeart.startAccept()
            }

        }

        binding = FragmentServerBinding.inflate(inflater, container, false)



        ServerHeart.receiveYes=object :ServerHeart.ReceiveYes{
           override fun onResponseReceived(response: Response,mySocket: MySocket) {
               val id=mySocket.id
                when (response.cmd) {
                    TcpCmd.CMD_READ_FILE_START -> {
                        ServerHeart.dataScope.launch {
                            if (imgArray.isEmpty()) {
                                return@launch
                            }
                            try {
                                serverSend[id]=JpegSend(imgArray.removeAt(0))
                                serverSend[id]!!.jpegSeq  = response.pkgNo
                                ServerHeart.send(TcpCmd.ReplyFileStart(serverSend[id]!!.jpegSize, serverSend[id]!!.jpegSeq,id),mySocket)
                            } catch (e: Exception) {

                            }

                        }


                    }
                    TcpCmd.CMD_READ_FILE_DATA -> {
                        val start = toUInt(response.content)
                        val lap = serverSend[id]!!.jpegSize - start
                        if (lap > 0) {
                            if (lap >= mtu) {
                                ServerHeart.send(
                                    TcpCmd.ReplyFileData(
                                        serverSend[id]!!.jpegArray.copyOfRange(
                                            start,
                                            start + mtu
                                        ), response.pkgNo,
                                        id
                                    ),
                                    mySocket
                                )
                            } else {
                                ServerHeart.send(
                                    TcpCmd.ReplyFileData(
                                        serverSend[id]!!.jpegArray.copyOfRange(start, serverSend[id]!!.jpegSize),
                                        response.pkgNo,
                                        id
                                    ),
                                    mySocket
                                )
                            }
                        }
                    }

                }
            }
        }


        return binding.root
    }



    val imgArray = ArrayList<ByteArray>()







    override fun onDestroy() {
        server.close()
        super.onDestroy()
    }


}