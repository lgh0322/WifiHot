package com.example.wifihot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.Socket

object ClientHeart {
    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var  socket : Socket

    interface Receive{
        fun tcpReceive(byteArray: ByteArray)
    }

    var receive:Receive?=null


    fun startRead(){
        Thread{
            val buffer = ByteArray(2000)
            val input= socket.getInputStream()
            while(true){
                try {
                    val byteSize=input.read(buffer)
                    if(byteSize>0){
                        receive?.tcpReceive(buffer.copyOfRange(0,byteSize))
                    }
                }catch (e:Exception){
                    break
                }

            }
        }.start()
    }


    fun send(b:ByteArray){
        val output= socket.getOutputStream()
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