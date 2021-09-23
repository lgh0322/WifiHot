package com.example.wifihot

import android.util.Log
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.add
import com.example.wifihot.utiles.toUInt
import com.example.wifihot.utiles.unsigned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import kotlin.experimental.inv

object ServerHeart {

    interface ReceiveYes {
        fun onResponseReceived(response: Response, mySocket: MySocket)
    }

    var receiveYes: ReceiveYes? = null

    val dataScope = CoroutineScope(Dispatchers.IO)
    val availableId = BooleanArray(256) {
        true
    }

    private fun getAvailableId(): Int {
        for (k in availableId.indices) {
            if (availableId[k]) {
                availableId[k] = false
                return k
            }
        }
        return 0
    }



    fun poccessLinkData(mySocket: MySocket):ByteArray? {
        var bytes = mySocket.pool
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
                if (i + 11 + len > bytes.size) {
                    continue@loop
                }

                val temp: ByteArray = bytes.copyOfRange(i, i + 11 + len)
                if (temp.last() == CRCUtils.calCRC8(temp)) {
                    receiveYes?.onResponseReceived(Response(temp),mySocket)

                }
                val tempBytes: ByteArray? =
                    if (i + 11 + len == bytes.size) null else bytes.copyOfRange(
                        i + 11 + len,
                        bytes.size
                    )

                bytes=tempBytes
                con=true
                break@loop
            }
            if(!con){
                return bytes
            }else{
                con=false
            }

        }
        return null
    }

    fun startAccept() {

                val serverSocket = MySocket(Socket(NetInfo.server, NetInfo.port))

                startRead(serverSocket)


    }

    var timex=0L


    fun startRead(mySocket: MySocket) {
        dataScope.launch {
            val buffer = ByteArray(200000)
            val input = mySocket.socket.getInputStream()
            var live = true
            while (live) {
                try {
                    val byteSize = input.read(buffer)
                    if (byteSize > 0) {
                        timex=System.currentTimeMillis()
                        val bytes=buffer.copyOfRange(0,byteSize)
                        mySocket.pool=add(mySocket.pool,bytes)
                        mySocket.pool= poccessLinkData(mySocket)
                    }
                } catch (e: Exception) {
                    availableId[mySocket.id] = true
                    try {
                        mySocket.socket.close()
                    } catch (e: java.lang.Exception) {

                    }
                    live = false
                }
                delay(5)

            }
        }
    }


    fun send(b: ByteArray, mySocket: MySocket) {
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

fun  ArrayList<Byte>.addAll(elements: ByteArray) {
    for(k in elements){
        this.add(k)
    }
}
