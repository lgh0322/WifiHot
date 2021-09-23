package com.example.wifihot

import android.util.Log
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.net.Socket
import java.util.*
import kotlin.experimental.inv

object ClientHeart {
    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var mySocket: MySocket

    interface Receive {
        fun onResponseReceived(response: Response, mySocket: MySocket)
    }

    var receive: Receive? = null

    fun poccessLinkData(mySocket: MySocket): ByteArray? {
        var bytes = mySocket.pool
        while (true) {
            if (bytes == null || bytes.size < 11) {
                break
            }
            var con = false

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
                    receive?.onResponseReceived(Response(temp), mySocket)
                    val tempBytes: ByteArray? =
                        if (i + 11 + len == bytes.size) null else bytes.copyOfRange(
                            i + 11 + len,
                            bytes.size
                        )

                    bytes = tempBytes
                    con = true

                    break@loop
                }

            }
            if (!con) {
                return bytes
            } else {
                con = false
            }

        }
        return null
    }


    val fuckLock = Mutex()

    fun startRead() {
        ClientHeart.dataScope.launch {
            val buffer = ByteArray(200000)
            var input = mySocket.socket.getInputStream()
            while (true) {
                try {


                    val byteSize = input.read(buffer)
                    if (byteSize > 0) {
                        val bytes = buffer.copyOfRange(0, byteSize)
                        mySocket.pool = add(mySocket.pool, bytes)
                        var time = System.currentTimeMillis()
                        mySocket.pool = poccessLinkData(mySocket)
                        val fuck = System.currentTimeMillis() - time
                        Log.e("fuckTime", fuck.toString())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        mySocket.socket.close()
                    }catch (ert:java.lang.Exception){

                    }
                    do{
                        try {
                            delay(1000)
                            mySocket = MySocket(Socket(NetInfo.server,NetInfo.port))
                            input = mySocket.socket.getInputStream()
                            break;
                        }catch (ew:Exception){

                        }
                    }while (true)


                }
                delay(5)
            }
        }
    }


    fun send(b: ByteArray) {
        val output = mySocket.socket.getOutputStream()
        output.write(b)
        output.flush()
    }

    fun byteArray2String(byteArray: ByteArray): String {
        var fuc = ""
        for (b in byteArray) {
            val st = String.format("%02X", b)
            fuc += ("$st  ");
        }
        return fuc
    }
}

fun LinkedList<Byte>.addAll(elements: ByteArray) {
    for (k in elements) {
        this.add((k))
    }

}
