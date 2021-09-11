package com.example.wifihot.fragment

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.BleServer
import com.example.wifihot.BleServer.socket
import com.example.wifihot.databinding.FragmentMainBinding
import com.example.wifihot.databinding.FragmentServerBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.Socket
import java.util.ArrayList

class ServerFragment : Fragment() {
    lateinit var binding: FragmentServerBinding


    lateinit var wifiManager: WifiManager
    private val PORT = 9999
    private val mList: List<Socket> = ArrayList()
    lateinit var server: ServerSocket

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        wifiManager = requireActivity().getSystemService(Context.WIFI_SERVICE) as WifiManager


        BleServer.dataScope.launch {
            server = ServerSocket(9999)
            socket = server.accept()
            BleServer.startRead()
        }

        binding = FragmentServerBinding.inflate(inflater, container, false)
        return binding.root
    }

}