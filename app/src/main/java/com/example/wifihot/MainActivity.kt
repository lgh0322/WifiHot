package com.example.wifihot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.wifihot.databinding.ActivityMainBinding
import com.example.wifihot.utiles.PathUtil

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PathUtil.initVar(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}