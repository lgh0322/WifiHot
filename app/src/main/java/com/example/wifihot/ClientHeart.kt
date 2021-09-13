package com.example.wifihot

import android.util.Log
import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.unsigned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.Socket
import kotlin.experimental.inv

object ClientHeart {
    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var  mySocket : MySocket

    interface Receive{
        fun onResponseReceived(response: Response, mySocket: MySocket)
    }

    var receive:Receive?=null

    fun poccessLinkData(mySocket: MySocket) {
        val linkData = mySocket.pool
        while (linkData.size >= 9) {
            val head = linkData[0]
            val cmd = linkData[1]
            val cmdInv = linkData[2]
            val len1 = linkData[6]
            val len2 = linkData[7]
            val len = len1.unsigned() + len2.unsigned().shl(8) + 9
            if (head == 0xA5.toByte()) {
                if (cmd == cmdInv.inv()) {
                    if (linkData.size >= len) {
                        val byteArray = ByteArray(len) {
                            linkData[it]
                        }
                        if (CRCUtils.calCRC8(byteArray) == byteArray[len - 1]) {
                            val bleResponse = Response(byteArray)
                            receive?.onResponseReceived(bleResponse, mySocket)
                        }
                        for (k in 0 until len) {
                            linkData.removeAt(0)
                        }
                    } else {
                        break;
                    }
                }
            } else {
                while (linkData.size > 0) {
                    if (linkData[0] != 0xA5.toByte()) {
                        linkData.removeAt(0)
                    } else {
                        break
                    }
                }
            }
        }
    }


    fun startRead(){
        Thread{
            val buffer = ByteArray(200000)
            val input= mySocket.socket.getInputStream()
            while(true){
                try {
                    val byteSize = input.read(buffer)
                    if (byteSize > 0) {
                        Log.e("metSpeed",byteSize.toString())
                    }
                }catch (e:Exception){
                    break
                }

            }
        }.start()
    }


    fun send(b:ByteArray){
        val output= mySocket.socket.getOutputStream()
        output.write(b)
        output.flush()
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