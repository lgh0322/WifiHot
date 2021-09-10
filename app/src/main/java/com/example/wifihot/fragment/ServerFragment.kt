package com.example.wifihot.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.databinding.FragmentMainBinding
import com.example.wifihot.databinding.FragmentServerBinding

class ServerFragment:Fragment() {
    lateinit var binding: FragmentServerBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentServerBinding.inflate(inflater,container,false)
        return binding.root
    }

}