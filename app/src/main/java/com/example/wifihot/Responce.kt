package com.example.wifihot

import com.example.wifihot.utiles.toUInt
import com.example.wifihot.utiles.unsigned

class Response(var bytes: ByteArray) {
    var cmd: Int = bytes[1].unsigned()
    var pkgType: Byte = bytes[3]
    var pkgNo: Int = bytes[4].unsigned()
    var len: Int = toUInt(bytes.copyOfRange(5, 7))
    var content: ByteArray = bytes.copyOfRange(7, 7 + len)
}