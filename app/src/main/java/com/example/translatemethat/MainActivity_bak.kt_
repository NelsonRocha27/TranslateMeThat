package com.example.translatemethat

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.translatemethat.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val REQUEST_SCREENSHOT = 59706
    private val mgr: MediaProjectionManager? = null

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

        var manager = mgr
        manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startActivityForResult(
            manager.createScreenCaptureIntent(),
            this@MainActivity.REQUEST_SCREENSHOT
        )

        // Example of a call to a native method
        binding.sampleText.text = stringFromJNI()
    }

    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == this@MainActivity.REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                val i = Intent(this, ScreenshotService::class.java)
                    .putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(ScreenshotService.EXTRA_RESULT_INTENT, data)
                startService(i)
            }
        }
        finish()
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