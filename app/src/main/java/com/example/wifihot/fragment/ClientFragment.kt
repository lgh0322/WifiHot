package com.example.wifihot.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.wifihot.databinding.FragmentClientBinding
import com.example.wifihot.databinding.FragmentMainBinding

class ClientFragment:Fragment() {
    lateinit var binding: FragmentClientBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentClientBinding.inflate(inflater,container,false)
        return binding.root
    }

}