package com.example.wifihot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.wifihot.databinding.ActivityMainBinding
import com.example.wifihot.utiles.PathUtil
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)
        File(PathUtil.getPathX("fuck.txt")).writeBytes(byteArrayOf(0x67))
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}