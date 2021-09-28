package com.example.wifihot.server


import android.content.Context
import android.graphics.*
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
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
import com.example.wifihot.tcp.TcpCmd
import com.vaca.fuckh264.record.VideoRecorder
import com.vaca.fuckh264.record.genData
import com.vaca.fuckh264.record.safeList
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


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
        Executors.newFixedThreadPool(3)
    }

    val mtu = 1000

    var bitmap: Bitmap? = null





    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        wifiManager = MainApplication.application.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        ServerHeart.dataScope.launch {
            ServerHeart.startAccept()
            delay(5000)
            ServerHeart.send(
                    TcpCmd.ReplyVpsSpsPps(
                            VideoRecorder.vpsspspps!!, id
                    )
            )
            delay(1000)
            cango = true
        }

        binding = FragmentServerBinding.inflate(inflater, container, false)

        startBackgroundThread()
        start(1080, 1920, 800000, surfaceCallback = {
            mySurface = it
            openCamera()
        })




        ServerHeart.receiveYes = object : ServerHeart.ReceiveYes {
            override fun onResponseReceived(response: Response, mySocket: MySocket) {
                val id = mySocket.id
                when (response.cmd) {
                    TcpCmd.CMD_READ_FILE_START -> {
                        ServerHeart.dataScope.launch {

                            try {

                            } catch (e: Exception) {

                            }

                        }


                    }
                    TcpCmd.CMD_READ_FILE_DATA -> {

                    }

                }
            }
        }


        return binding.root
    }

    var cango = false

    val imgArray = ArrayList<ByteArray>()


    private fun startBackgroundThread() {
        mHandlerThread = HandlerThread("fuck")
        mHandlerThread!!.start()
        mHandler = Handler(mHandlerThread!!.looper)
    }


    private val mCameraDeviceStateCallback: CameraDevice.StateCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    startPreview(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.e("fuckCamera", "a1")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("fuckCamera", "a2")
                    camera.close()
                }

                override fun onClosed(camera: CameraDevice) {
                    Log.e("fuckCamera", "a3")
                    camera.close()
                }
            }

    private fun openCamera() {
        try {
            val cameraManager =
                    requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private val mSessionStateCallback: CameraCaptureSession.StateCallback =
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    mCaptureSession = session
                    updatePreview()

                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            }

    private fun startPreview(camera: CameraDevice) {
        mPreviewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

        mPreviewBuilder.addTarget(mySurface)


        camera.createCaptureSession(
                Arrays.asList(mySurface),
                mSessionStateCallback,
                mHandler
        )
    }

    private val isRecording = safeList<Int>()
    val videoFormats = safeList<MediaFormat>()

    fun start(
            width: Int, height: Int, bitRate: Int, frameRate: Int = 15,
            frameInterval: Int = 15,
            surfaceCallback: (surface: Surface) -> Unit
    ) {
        isRecording.add(1)
        val videoRecorder = VideoRecorder(width, height, bitRate, frameRate,
                frameInterval, isRecording, surfaceCallback, { frame, timeStamp, bufferInfo, data ->
            val byteArray = data.genData()
            if (cango) {
                ServerHeart.dataScope.launch {
                    ServerHeart.send(TcpCmd.ReplyFrame(byteArray, 0, timeStamp))
                }
            }


        }, {
            videoFormats.add(it)
        })
        recorderThread.execute(videoRecorder)

    }



    var time = System.currentTimeMillis()
    var count = 0




    private fun updatePreview() {
        mHandler.post(Runnable {
            try {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        })
    }


    override fun onDestroy() {
        closeCamera()
        super.onDestroy()
    }

    private fun closeCamera() {
        try {
            mCaptureSession.stopRepeating()
            mCaptureSession.close()
        } catch (e: Exception) {

        }

        mCameraDevice!!.close()

        stopBackgroundThread()
    }

    private fun stopBackgroundThread() {
        try {
            if (mHandlerThread != null) {
                mHandlerThread!!.quitSafely()
                mHandlerThread!!.join()
                mHandlerThread = null
            }
            mHandler.removeCallbacksAndMessages(null)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

}