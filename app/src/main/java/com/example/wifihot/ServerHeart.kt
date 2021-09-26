package com.example.wifihot

import com.example.wifihot.utiles.CRCUtils
import com.example.wifihot.utiles.toUInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.*
import kotlin.experimental.inv

object ServerHeart {

    interface ReceiveYes {
        fun onResponseReceived(response: Response)
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


    fun poccessLinkData(mySocket: MySocket): ByteArray? {
        var bytes = mySocket.pool
        while (true) {
            if (bytes == null || bytes.size < 11) {
                break
            }
            var con = false

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
                    receiveYes?.onResponseReceived(Response(temp))
                    val tempBytes: ByteArray? =
                        if (i + 11 + len == bytes.size) null else bytes.copyOfRange(
                            i + 11 + len,
                            bytes.size
                        )

                    bytes = tempBytes
                    con = true
                    break@loop
                }

            }
            if (!con) {
                return bytes
            } else {
                con = false
            }

        }
        return null
    }


    var timex = 0L


    fun send(b: ByteArray) {
        DatagramSocket(NetInfo.port).use {
            if (b.size > NetInfo.mtu) {
                val num = b.size / NetInfo.mtu
                for (k in 0 until num) {
                    val sendPacket = b.copyOfRange(k * NetInfo.mtu, (k + 1) * NetInfo.mtu)
                    val outPacket = DatagramPacket(
                        sendPacket,
                        sendPacket.size,
                        InetAddress.getByName(NetInfo.client),
                        NetInfo.port
                    )
                    it.send(outPacket)
                }
                val sendPacket = b.copyOfRange(num * NetInfo.mtu, b.size)
                val outPacket = DatagramPacket(
                    sendPacket,
                    sendPacket.size,
                    InetAddress.getByName(NetInfo.client),
                    NetInfo.port
                )
                it.send(outPacket)
            } else {
                val outPacket =
                    DatagramPacket(b, b.size, InetAddress.getByName(NetInfo.client), NetInfo.port)
                it.send(outPacket)
            }

        }


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

fun ArrayList<Byte>.addAll(elements: ByteArray) {
    for (k in elements) {
        this.add(k)
    }
}
