package com.example.wifihot

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket

object BleServer {
    val dataScope = CoroutineScope(Dispatchers.IO)
    lateinit var  socket : Socket

    interface Receive{
        fun tcpReceive(byteArray: ByteArray)
    }

    var receive:Receive?=null


    fun startRead(){
        Thread{
            socket = Socket("81.71.163.52",5555)
            var input= socket.getInputStream()
            val buffer = ByteArray(20000)
            while(true){
                try {
                    val bytes=input.read(buffer)
                    if(bytes>0){
                        receive?.tcpReceive(buffer.copyOfRange(0,bytes))
                    }
                    Thread.sleep(10)
                }catch (eer:Exception){
                    do {
                        try {
                            Thread.sleep(1000)
                            socket = Socket("81.71.163.52",5555)
                            input= socket.getInputStream()
                            break;
                        }catch (ewr:java.lang.Exception){

                        }
                    }while (true)
                }

            }
        }.start()
    }


    fun send(b:ByteArray){
        dataScope.launch {
            try {
                val output= socket.getOutputStream()
                output.write(b)
                output.flush()
            }catch (e:java.lang.Exception){

            }

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