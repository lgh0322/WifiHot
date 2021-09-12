package com.example.wifihot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket

object ServerHeart {
    val dataScope = CoroutineScope(Dispatchers.IO)
   val  socketList=ArrayList<Socket>()
    lateinit var server: ServerSocket

    interface Receive{
        fun tcpReceive0(byteArray: ByteArray,index:Int)
        fun tcpReceive1(byteArray: ByteArray,index:Int)
    }

    var receive:Receive?=null


    suspend fun startAccept(){
        while (true){
            try {
                val socket = server.accept()
                socketList.add(socket)
                startRead(socket)
            } catch (e: Exception) {

            }
        }
    }


    suspend fun startRead(socket:Socket){
       dataScope.launch{
            val buffer = ByteArray(2000)
            val input= socket.getInputStream()
           val id=socketList.indexOf(socket)
            while(true){
                try {
                    val byteSize=input.read(buffer)
                    if(byteSize>0){
                        if(id==0){
                            receive?.tcpReceive0(buffer.copyOfRange(0,byteSize),id )
                        }else{
                            receive?.tcpReceive1(buffer.copyOfRange(0,byteSize),id )
                        }

                    }
                }catch (e:Exception){
                    socketList.remove(socket)
                    break
                }

            }
        }
    }


    fun send(b:ByteArray,index:Int){
        val socket= socketList[index]
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