package org.mg.conferenceapp.media

import android.content.Context
import android.util.Log
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.EglBase
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

// Wrapper for the device's camera
class Camera(
    context: Context,
    mediaCommon: MediaCommon
) : AutoCloseable {

    val TAG = "Camera"

    val eglBase: EglBase;
    val camCapturer: CameraVideoCapturer
    val videoSource: VideoSource
    val surfaceTextureHelper: SurfaceTextureHelper
    val videoTrack: VideoTrack

    init {
        eglBase = EglBase.create()

        val camEnumerator = if(Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator()
        }

        val deviceNames = camEnumerator.deviceNames

        if(deviceNames.isEmpty()) {
            throw RuntimeException("No cameras available")
        }

        Log.i(TAG, "Available cameras: " + deviceNames.joinToString { name -> name })

        var selectedDevice = deviceNames[0]

        for(device in deviceNames) {
            if(camEnumerator.isFrontFacing(device)) {
                selectedDevice = device
            }
        }

        Log.i(TAG, "Selected camera: $selectedDevice")

        camCapturer = camEnumerator.createCapturer(
            selectedDevice,
            object : CameraVideoCapturer.CameraEventsHandler {
                override fun onCameraError(err: String?) {
                    Log.i(TAG, "onCameraError: $err")
                }

                override fun onCameraDisconnected() {
                    Log.i(TAG, "onCameraDisconnected")
                }

                override fun onCameraFreezed(err: String?) {
                    Log.i(TAG, "onCameraFreezed: $err")
                }

                override fun onCameraOpening(msg: String?) {
                    Log.i(TAG, "onCameraOpening: $msg")
                }

                override fun onFirstFrameAvailable() {
                    Log.i(TAG, "onFirstFrameAvailable")
                }

                override fun onCameraClosed() {
                    Log.i(TAG, "onCameraClosed")
                }

            }
        ) ?: throw RuntimeException("Failed to create cam capturer")

        videoSource = mediaCommon.peerConnectionFactory.createVideoSource(false)

        surfaceTextureHelper = SurfaceTextureHelper.create(
            "webcamThread",
            eglBase.eglBaseContext)

        camCapturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        camCapturer.startCapture(640, 480, 25)

        videoTrack = mediaCommon.peerConnectionFactory.createVideoTrack("webcam", videoSource)
    }

    override fun close() {
        Log.i(TAG, "Shutting down camera")
        videoTrack.dispose()
        surfaceTextureHelper.dispose()
        videoSource.dispose()
        camCapturer.stopCapture()
        camCapturer.dispose()
        eglBase.release()
    }
}