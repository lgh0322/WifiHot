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
import java.net.ServerSocket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.experimental.inv

object ServerHeart {

    interface ReceiveYes {
        fun onResponseReceived(response: Response, mySocket: MySocket)
    }

    var receiveYes: ReceiveYes? = null

    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var server: ServerSocket
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


    var gh:Response?=null

    lateinit var mySocket:MySocket

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
                receiveYes?.onResponseReceived(Response(temp),mySocket)
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



    fun startAccept() {
        while (true) {
            try {
                val serverSocket = MySocket(server.accept(), getAvailableId())

                startRead(serverSocket)
            } catch (e: Exception) {

            }
        }
    }

    val gg=LinkedList<ByteArray>()
    val wawa= Mutex()

    fun startRead(mySocket: MySocket) {
        dataScope.launch {

            var live = true
            while (live) {
                try {
                    val buffer = ByteArray(2000)
                    val input = mySocket.socket.getInputStream()
                    val byteSize = input.read(buffer)
                    if (byteSize > 0) {
                        val bytes=buffer.copyOfRange(0,byteSize)
                        gg.offer(bytes)
                        GlobalScope.launch {
                            wawa.withLock {
                                mySocket.pool= add(mySocket.pool, gg.poll())
                                this@ServerHeart.mySocket=mySocket
                                mySocket.pool= handleDataPool(mySocket.pool)
                            }
                        }


                    }
                } catch (e: Exception) {
                    availableId[mySocket.id] = true
                    try {
                        mySocket.socket.close()
                    } catch (e: java.lang.Exception) {

                    }
                    live = false
                }

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
