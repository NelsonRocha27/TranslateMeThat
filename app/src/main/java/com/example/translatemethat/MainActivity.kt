package com.example.translatemethat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.translatemethat.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            askPermission()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startService(Intent(this@MainActivity, FloatingViewService::class.java))
            finish()
        } else if (Settings.canDrawOverlays(this)) {
            startService(Intent(this@MainActivity, FloatingViewService::class.java))
            finish()
        } else {
            askPermission()
            Toast.makeText(
                this,
                "You need System Alert Window Permission to do this",
                Toast.LENGTH_SHORT
            ).show()
        }

        Log.e("MyTag", "Log.e() example")

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}