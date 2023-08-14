package com.renekakpo.textsnap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.renekakpo.textsnap.databinding.ActivityMainBinding
import java.io.IOException


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var mCameraSource: CameraSource

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val requestPermissionID = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCameraSource()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode != requestPermissionID) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                mCameraSource.start(binding.cameraView.holder)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun startCameraSource() {
        try {

            //Create the TextRecognizer
            val textRecognizer = TextRecognizer.Builder(applicationContext).build()

            mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build()

            if (!textRecognizer.isOperational) {
                Log.w(TAG, "Detector dependencies not loaded yet")
            } else {
                //Initialize cameraSource to use high resolution and set Autofocus on.
                mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build()

                /**
                 * Add call back to SurfaceView and check if camera permission is granted.
                 * If permission is granted we can start our cameraSource and pass it to surfaceView
                 */
                binding.cameraView.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            if (ActivityCompat.checkSelfPermission(
                                    applicationContext,
                                    Manifest.permission.CAMERA
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this@MainActivity, arrayOf(Manifest.permission.CAMERA),
                                    requestPermissionID
                                )
                                return
                            }
                            mCameraSource.start(binding.cameraView.holder)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {
                    }

                    /**
                     * Release resources for cameraSource
                     */
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        mCameraSource.stop()
                    }
                })

                //Set the TextRecognizer's Processor.
                textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
                    override fun release() {}

                    /**
                     * Detect all the text from camera using TextBlock and the values into a stringBuilder
                     * which will then be set to the textView.
                     */
                    override fun receiveDetections(detections: Detections<TextBlock>) {
                        val items = detections.detectedItems
                        if (items.size() != 0) {
                            binding.textView.post {
                                val stringBuilder = StringBuilder()
                                for (i in 0 until items.size()) {
                                    val item = items.valueAt(i)
                                    stringBuilder.append(item.value)
                                    stringBuilder.append("\n")
                                }
                                binding.textView.text = stringBuilder.toString()
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("startCameraSource", "${e.message}")
        }
    }
}