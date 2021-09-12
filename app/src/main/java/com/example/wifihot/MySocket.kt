package com.example.wifihot

import java.net.Socket

class MySocket(val socket:Socket, var id:Int=0) {
    var pool=ArrayList<Byte>()
}