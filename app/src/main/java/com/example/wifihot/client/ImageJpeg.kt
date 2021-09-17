package com.example.wifihot.client

class ImageJpeg(val size:Int) {
    val content=ByteArray(size){
        0
    }

    var index=0
    fun add(byteArray: ByteArray){
        for(k in byteArray.indices){
            content[index+k]=byteArray[k]
        }
        index+=byteArray.size
    }
}