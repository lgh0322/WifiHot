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
import androidx.fragment.app.Fragment
import com.example.wifihot.BleServer
import com.example.wifihot.BleServer.socket
import com.example.wifihot.Response
import com.example.wifihot.tcp.TcpCmd
import com.example.wifihot.databinding.FragmentServerBinding
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.*
import okhttp3.internal.closeQuietly
import java.io.ByteArrayOutputStream
import java.lang.Runnable
import java.net.ServerSocket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.inv

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


    lateinit var wifiManager: WifiManager
    private val PORT = 9999
    lateinit var server:ServerSocket


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
    private var pool: ByteArray? = null

    val mtu = 60000

    var bitmap: Bitmap? = null
    lateinit var jpegArray: ByteArray
    var jpegSize = 0
    var jpegIndex = 0
    var jpegSeq = 0;
    lateinit var acceptJob:Job

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager


        BleServer.dataScope.launch {
             server = ServerSocket(PORT)

            acceptJob=BleServer.dataScope.launch {
                try {
                    socket = server.accept()
                    BleServer.startRead()
                }catch (e:Exception){

                }

            }

        }

        binding = FragmentServerBinding.inflate(inflater, container, false)

        startBackgroundThread()
        openCamera()

        BleServer.receive = object : BleServer.Receive {
            override fun tcpReceive(byteArray: ByteArray) {
                byteArray.apply {
                    pool = add(pool, this)
                }
                pool?.apply {
                    pool = handleDataPool(pool)
                }
            }

        }

        return binding.root
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


    val imgArray=ArrayList<ByteArray>()

    private fun onResponseReceived(response: Response) {

        when (response.cmd) {
            TcpCmd.CMD_READ_FILE_START -> {
                BleServer.dataScope.launch {
                        jpegIndex = 0
                        jpegArray=imgArray.last()
                        jpegSeq = response.pkgNo
                        jpegSize = jpegArray.size
                        BleServer.send(TcpCmd.ReplyFileStart(jpegSize, jpegSeq))
                }


            }
            TcpCmd.CMD_READ_FILE_DATA -> {
                val start = toUInt(response.content)
                val lap = jpegSize - start
                if (lap > 0) {
                    if (lap >= mtu) {
                        BleServer.send(
                            TcpCmd.ReplyFileData(
                                jpegArray.copyOfRange(
                                    start,
                                    start + mtu
                                ), response.pkgNo
                            )
                        )
                    } else {
                        BleServer.send(
                            TcpCmd.ReplyFileData(
                                jpegArray.copyOfRange(start, jpegSize),
                                response.pkgNo
                            )
                        )
                        BleServer.dataScope.launch {
                            while(imgArray.size>5){
                                imgArray.removeAt(0)
                            }
                        }


                    }
                }
            }

        }
    }


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
        mImageReader = ImageReader.newInstance(
            mPreviewSize.width,
            mPreviewSize.height,
            ImageFormat.YUV_420_888,
            2 /*最大的图片数，mImageReader里能获取到图片数，但是实际中是2+1张图片，就是多一张*/
        )

        mPreviewBuilder.addTarget(mImageReader.surface)
        mImageReader.setOnImageAvailableListener(
            { reader ->
                mHandler.post(ImageSaver(reader))
            }, mHandler
        )


        camera.createCaptureSession(
            Arrays.asList(mImageReader.surface),
            mSessionStateCallback,
            mHandler
        )
    }


    private fun YUV_420_888toNV21(image: Image): ByteArray {
        val nv21: ByteArray
        val yBuffer = image.planes[0].buffer
        val vuBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()
        nv21 = ByteArray(ySize + vuSize)
        yBuffer[nv21, 0, ySize]
        vuBuffer[nv21, ySize, vuSize]
        return nv21
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 30, out)
        return out.toByteArray()
    }

    var time = System.currentTimeMillis()
    var count = 0


    private inner class ImageSaver(var reader: ImageReader) : Runnable {
        override fun run() {
            BleServer.dataScope.launch {
                val image = reader.acquireLatestImage()
                if (image == null) {
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    count++
                    if (count >= 10) {
                        val x = (System.currentTimeMillis() - time).toFloat() / 1000f
//                        binding.fps.text = (10f / x).toInt().toString() + " fps"
                        time = System.currentTimeMillis()
                        count = 0
                    }
                }
                try {
                    val data = NV21toJPEG(
                        YUV_420_888toNV21(image),
                        image.width, image.height
                    );


                    imgArray.add(data.clone())
                    if(imgArray.size>10){
                        imgArray.removeAt(0)
                    }
                }catch (e:Exception){

                }




                image.close()

            }
        }
    }


    private fun updatePreview() {
        mHandler.post(Runnable {
            try {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        })
    }

    override fun onPause() {
        closeCamera()
        server.close()
        super.onPause()

    }

    private fun closeCamera() {
        mCaptureSession.stopRepeating()
        mCaptureSession.close()
        mCameraDevice!!.close()
        mImageReader.close()
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