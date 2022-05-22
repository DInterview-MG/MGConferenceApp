package org.mg.conferenceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.mg.conferenceapp.media.CallManager
import org.mg.conferenceapp.signaling.RemoteTrack
import org.mg.conferenceapp.signaling.RemoteTrackType
import org.mg.conferenceapp.ui.RemoteTrackAdapter
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val callManager = SingleThreadVar<CallManager?>(null)
    private val eglBase = SingleThreadVar<EglBase?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Activity created")

        eglBase.set(EglBase.create())

        // If we don't have the required permissions, let's request them.
        checkAndRequestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        callManager.get()?.close()
        eglBase.get()?.release()
    }

    private fun displayError(msg: String) {
        Log.e(TAG, "Showing error toast: '$msg'")
        Utils.MAIN_HANDLER.post {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Checks if all requried permissions are granted
    private fun arePermissionsGranted() : Boolean {
        for(permission in requiredPermissions) {
            if(ContextCompat.checkSelfPermission(this, permission)
                    !=  PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun checkAndRequestPermissions() {

        if(arePermissionsGranted()) {
            onPermissionsGranted()

        } else {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for(result in grantResults) {
            if(result != PackageManager.PERMISSION_GRANTED) {
                displayError("Permissions were not granted")
                return
            }
        }

        onPermissionsGranted()
    }

    private fun onPermissionsGranted() {
        Log.i(TAG, "Permission now granted")
        getUserParams()
    }

    // Shows the initial UI with the hostname/port text fields.
    private fun getUserParams() {

        setContentView(R.layout.activity_main_preconnect)

        val hostname: AppCompatEditText = findViewById(R.id.editTextHostname)
        val port: AppCompatEditText = findViewById(R.id.editTextPort)
        val connect: AppCompatButton = findViewById(R.id.connectButton)

        connect.setOnClickListener {
            try {
                joinRoom(hostname.text.toString(), port.text.toString().toInt())
            } catch(E: NumberFormatException) {
                displayError("Port must be a number")
            }
        }
    }

    // Initiates the process of connecting to the signaling server and joining the room.
    private fun joinRoom(
        hostname: String,
        port: Int
    ) {
        setContentView(R.layout.activity_main)

        val videoTrackSpinner: AppCompatSpinner = findViewById(R.id.videoTrackSpinner)
        val audioTrackSpinner: AppCompatSpinner = findViewById(R.id.audioTrackSpinner)
        val surfaceViewRenderer : SurfaceViewRenderer = findViewById(R.id.surfaceViewRenderer)

        val videoTrackAdapter = RemoteTrackAdapter(this, RemoteTrackType.VIDEO)
        val audioTrackAdapter = RemoteTrackAdapter(this, RemoteTrackType.AUDIO)

        videoTrackSpinner.adapter = videoTrackAdapter
        audioTrackSpinner.adapter = audioTrackAdapter

        callManager.set(CallManager(
            applicationContext,
            hostname,
            port,
            this::displayError,
            { tracks ->
                Log.i(TAG, "Got updated tracks")
                videoTrackAdapter.update(tracks)
                audioTrackAdapter.update(tracks)
            })
        )

        surfaceViewRenderer.init(
            eglBase.get()!!.eglBaseContext,
            object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.i(TAG, "Surface: onFirstFrameRendered")
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    Log.i(TAG, "Surface: onFrameResolutionChanged ($p0, $p1, $p2)")
                }

            }
        )

        videoTrackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onVideoTrackSelected(
                    videoTrackAdapter.getItem(position) as RemoteTrack?,
                    surfaceViewRenderer)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                onVideoTrackSelected(null, surfaceViewRenderer)
            }
        }

        audioTrackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onAudioTrackSelected(
                    audioTrackAdapter.getItem(position) as RemoteTrack?)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                onAudioTrackSelected(null)
            }
        }
    }

    private fun onVideoTrackSelected(
        track: RemoteTrack?,
        view: SurfaceViewRenderer
    ) {
        callManager.get()?.recvVideoTrack(track, view)
    }

    private fun onAudioTrackSelected(track: RemoteTrack?) {
        callManager.get()?.recvAudioTrack(track)
    }
}