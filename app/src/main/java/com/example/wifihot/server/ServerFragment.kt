package com.example.wifihot.server


import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.MediaFormat
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.*
import com.example.wifihot.databinding.FragmentServerBinding
import com.example.wifihot.audio.AudioEncoder
import com.jadyn.mediakit.audio.AudioPacket
import com.jadyn.mediakit.audio.AudioRecorder
import com.vaca.fuckh264.record.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding

    val aacDecoderUtil=AACDecoderUtil()

    lateinit var wifiManager: WifiManager
    private val PORT = 9999

    lateinit var mySurface: Surface

    private val mCameraId = "0"
    lateinit var mPreviewSize: Size
    private val PREVIEW_WIDTH = 1920
    private val PREVIEW_HEIGHT = 1080
    private var mCameraDevice: CameraDevice? = null
    lateinit var mHandler: Handler
    lateinit var mCaptureSession: CameraCaptureSession
    lateinit var mPreviewBuilder: CaptureRequest.Builder
    private var mHandlerThread: HandlerThread? = null

    private var pool0: ByteArray? = null
    private var pool1: ByteArray? = null
    private val recorderThread by lazy {
        Executors.newFixedThreadPool(10)
    }

    val mtu = 1000

    var bitmap: Bitmap? = null

    val TAG="fuck"

    private val audioQueue by lazy {
        ArrayBlockingQueue<ByteArray>(20)
    }
    val audioFormats = safeList<MediaFormat>()


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        wifiManager = MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        aacDecoderUtil.start()


        binding = FragmentServerBinding.inflate(inflater, container, false)

        isRecording.add(1)

        // 执行音频录制，回调PCM数据
        recorderThread.execute(AudioRecorder(isRecoding = isRecording, dataCallBack = { size, data ->
            Log.d(TAG, "audio pcm size : $size data :${data.size}: ")
            audioQueue.offer(data)
        }))
        // 执行音频编码，将PCM数据编码为AAC数据
        recorderThread.execute(AudioEncoder(isRecording, createAMRFormat(),
                audioQueue, { byteBuffer, bufferInfo ->
            val data = ByteArray(byteBuffer.remaining())
            byteBuffer.get(data, 0, data.size)
            val audioPacket = AudioPacket(data, data.size, bufferInfo.copy())
                Log.e("fuckyou",byteArray2String(data))
                aacDecoderUtil.decode(data,0,data.size,bufferInfo.presentationTimeUs)

                framex++
                if(framex>=100){
                    framex=0
                    val gg=(System.currentTimeMillis()-timex).toFloat()/1000f
                    Log.e("gaga",(data.size*(100f/(gg)).toInt()).toString())
                    timex=System.currentTimeMillis()
                }

        },{
            // 得到输出的audio format
            audioFormats.add(it)
        }) )


        return binding.root
    }

    var cango = false

    var ga=0

    var framex=0
    var timex=System.currentTimeMillis()


    private val isRecording = safeList<Int>()
    val videoFormats = safeList<MediaFormat>()

    fun start(
            width: Int, height: Int, bitRate: Int, frameRate: Int = 15,
            frameInterval: Int = 15,
            surfaceCallback: (surface: Surface) -> Unit
    ) {


    }






    fun byteArray2String(byteArray: ByteArray):String {
        var fuc=""
        for (b in byteArray) {
            val st = String.format("%02X", b)
            fuc+=("$st  ");
        }
        return fuc
    }



}