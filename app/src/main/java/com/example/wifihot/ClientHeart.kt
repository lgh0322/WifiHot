package com.example.wifihot

import android.util.Log
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import com.example.wifihot.utiles.unsigned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.Socket
import java.util.*
import kotlin.experimental.inv

object ClientHeart {
    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var  mySocket : MySocket

    interface Receive{
        fun onResponseReceived(response: Response, mySocket: MySocket)
    }

    var receive:Receive?=null


    var gh:Response?=null


    private fun handleDataPool(bytes: ByteArray?): ByteArray? {
        val bytesLeft: ByteArray? = bytes

        if (bytes == null || bytes.size < 11) {
            return bytes
        }
        loop@ for (i in 0 until bytes.size - 10) {
            if (bytes[i] != 0xA5.toByte() || bytes[i + 1] != bytes[i + 2].inv()) {
                continue@loop
            }

            // need content length
            val len = toUInt(bytes.copyOfRange(i + 6, i + 10))
            if (i + 11 + len > bytes.size) {
                continue@loop
            }

            val temp: ByteArray = bytes.copyOfRange(i, i + 11 + len)
            if (temp.last() == CRCUtils.calCRC8(temp)) {
                receive?.onResponseReceived(Response(temp), mySocket)
                val tempBytes: ByteArray? =
                    if (i + 11 + len == bytes.size) null else bytes.copyOfRange(
                        i + 11 + len,
                        bytes.size
                    )

                return handleDataPool(tempBytes)
            }
        }

        return bytesLeft
    }










    val lao= Mutex()

    val gg=LinkedList<ByteArray>()

    fun startRead(){
        Thread{

            while(true){
                try {
                    val buffer = ByteArray(200000)
                    val input= mySocket.socket.getInputStream()
                    val byteSize = input.read(buffer)
                    if (byteSize > 0) {
                        val bytes=buffer.copyOfRange(0,byteSize)
                        Log.e("fuckSIze5555555555555", byteSize.toString())
                       gg.offer(bytes)
                        GlobalScope.launch {
                            lao.withLock {
                                try {
                                    mySocket.pool= add(mySocket.pool,gg.poll())
                                    Log.e("fuckSIze", mySocket.pool!!.size.toString())
                                    val time=System.currentTimeMillis()

                                    mySocket.pool= handleDataPool(mySocket.pool)

                                    val time2=System.currentTimeMillis()-time
                                    Log.e("fuckfuckxx",time2.toString())
                                }catch (e:java.lang.Exception){

                                }

                            }

                        }

                    }
                }catch (e:Exception){
                    break
                }

            }
        }.start()
    }


    fun send(b:ByteArray){
        try {
            val output= mySocket.socket.getOutputStream()
            output.write(b)
            output.flush()
        }catch (e:java.lang.Exception){

        }

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