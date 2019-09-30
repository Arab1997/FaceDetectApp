package com.prattham.facedetectapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.prattham.facedetectapp.model.FaceDetectionModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity(), FrameProcessor {


    private val cameraFacing = Facing.FRONT
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var faceDetectionModels: ArrayList<FaceDetectionModel>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        faceDetectionModels = ArrayList()
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)

        //imageView = findViewById(R.id.face_detection_image_view)
        //faceDetectionCameraView = findViewById(R.id.face_detection_camera_view)

        //setup camera view from lib
        face_detection_camera_view.facing = cameraFacing
        face_detection_camera_view.setLifecycleOwner(this)
        face_detection_camera_view.addFrameProcessor(this)

        //bottomSheet behaviour
        bottom_sheet_button.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            else
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

    }

    override fun process(frame: Frame) {

    }
}
