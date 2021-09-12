package com.example.wifihot.client

class ImageJpeg(val size:Int) {
    val content=ByteArray(size){
        0
    }

    fun set(byteArray: ByteArray,index:Int){
        for(k in byteArray.indices){
            content[1000*index+k]=byteArray[k]
        }
    }
}