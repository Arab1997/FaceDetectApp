package com.prattham.facedetectapp

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.common.FirebaseVisionPoint
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor
import com.prattham.facedetectapp.adapter.FaceDetectionAdapter
import com.prattham.facedetectapp.model.FaceDetectionModel
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.toast


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

        //setup camera view from lib
        face_detection_camera_view.facing = cameraFacing
        face_detection_camera_view.setLifecycleOwner(this)
        face_detection_camera_view.addFrameProcessor(this)

        //bottomSheet behaviour
        bottom_sheet_button.setOnClickListener {
            //            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED)
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            else
//                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            CropImage.activity().start(this)
        }

        bottom_sheet_recycler_view.layoutManager = LinearLayoutManager(this)
        bottom_sheet_recycler_view.adapter = FaceDetectionAdapter(faceDetectionModels, this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val imageURI = result.uri
                try {
                    imageURI?.let {
                        if (Build.VERSION.SDK_INT < 28) {
                            analyseImage(
                                MediaStore.Images.Media.getBitmap(
                                    contentResolver,
                                    imageURI
                                )
                            )

                        } else {
                            analyseImage(ImageDecoder.createSource(contentResolver, imageURI))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun analyseImage(bitmap: Any) {
        if (bitmap == null) {
            toast("There was an error")
            return
        }
        face_detection_image_view.setImageBitmap(null)
        faceDetectionModels.clear()
        bottom_sheet_recycler_view.adapter?.notifyDataSetChanged()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        showProgress()

        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap as Bitmap)

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()

        val faceDetector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
        faceDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener {
                val mutableImage = bitmap.copy(Bitmap.Config.ARGB_8888, true)

                detectFaces(it, mutableImage)
            }
            .addOnFailureListener {

            }
    }

    private fun detectFaces(it: List<FirebaseVisionFace>, mutableImage: Bitmap?) {
        if (it == null || mutableImage == null) {
            toast("There was an error")
            return
        }
        val canvas = Canvas(mutableImage)

        val facePaint = Paint()
        facePaint.color = Color.GREEN
        facePaint.style = Paint.Style.STROKE
        facePaint.strokeWidth = 5f

        val faceTextPaint = Paint()
        faceTextPaint.color = Color.BLUE
        faceTextPaint.textSize = 30f
        faceTextPaint.typeface = Typeface.SANS_SERIF

        val landmarkPaint = Paint()
        landmarkPaint.color = Color.RED
        landmarkPaint.style = Paint.Style.FILL
        landmarkPaint.strokeWidth = 8f

        for (i in it.indices) {
            canvas.drawRect(it[i].boundingBox, facePaint)
            canvas.drawText(
                "Face $i",
                (((it[i].boundingBox.centerX() - (it[i].boundingBox.width() shr 2)) + 8f)), // added shr to avoid errors when dividing with "/"
                ((((it[i].boundingBox.centerY() + it[i].boundingBox.height()) shr 2)) - 8f),
                faceTextPaint
            )

            val face = it[i]  //get one face at a time

            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE) != null) {
                val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
                //leftEye is created, now we draw a circle to point at it
                canvas.drawCircle(leftEye!!.position.x, leftEye.position.y, 8f, landmarkPaint)
            }

            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE) != null) {
                val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
                //rightEye is created, now we draw a circle to point at it
                canvas.drawCircle(rightEye!!.position.x, rightEye.position.y, 8f, landmarkPaint)
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE) != null) {
                val noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)
                //noseBase is created, now we draw a circle to point at it
                canvas.drawCircle(noseBase!!.position.x, noseBase.position.y, 8f, landmarkPaint)
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR) != null) {
                val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                //leftEar is created, now we draw a circle to point at it
                canvas.drawCircle(leftEar!!.position.x, leftEar.position.y, 8f, landmarkPaint)
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR) != null) {
                val rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
                //rightEar is created, now we draw a circle to point at it
                canvas.drawCircle(rightEar!!.position.x, rightEar.position.y, 8f, landmarkPaint)
            }
            if (face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT) != null
                && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM) != null
                && face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT) != null
            ) {
                val leftMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT)
                val bottomMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
                val rightMouth = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT)
                canvas.drawLine(
                    leftMouth!!.position.x,
                    leftMouth.position.y,
                    bottomMouth!!.position.x,
                    bottomMouth.position.y,
                    landmarkPaint
                )
                canvas.drawLine(
                    bottomMouth.position.x,
                    bottomMouth.position.y,
                    rightMouth!!.position.x,
                    rightMouth.position.y,
                    landmarkPaint
                )
            }

            faceDetectionModels.add(
                FaceDetectionModel(
                    i,
                    "Smiling Probability ${face.smilingProbability}"
                )
            )
            faceDetectionModels.add(
                FaceDetectionModel(
                    i,
                    "Left Eye open Probability ${face.leftEyeOpenProbability}"
                )
            )
            faceDetectionModels.add(
                FaceDetectionModel(
                    i,
                    "Right Eye open  Probability ${face.rightEyeOpenProbability}"
                )
            )


        }//end

    }

    private fun showProgress() {
        bottom_sheet_button_image.visibility = View.GONE
        bottom_sheet_button_progress.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        bottom_sheet_button_image.visibility = View.VISIBLE
        bottom_sheet_button_progress.visibility = View.GONE
    }

    override fun process(frame: Frame) {

        val width = frame.size.width
        val height = frame.size.height

        val metadata = FirebaseVisionImageMetadata.Builder()
            .setWidth(width)
            .setHeight(height)
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setRotation(
                if ((cameraFacing === Facing.FRONT))
                    FirebaseVisionImageMetadata.ROTATION_270
                else
                    FirebaseVisionImageMetadata.ROTATION_90
            )
            .build()

        val firebaseVisionImage = FirebaseVisionImage.fromByteArray(frame.data, metadata)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()
        val faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        faceDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener {
                face_detection_image_view.setImageBitmap(null)

                val bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val dotPaint = Paint()
                dotPaint.color = Color.RED
                dotPaint.style = Paint.Style.FILL
                dotPaint.strokeWidth = 3f

                val linePaint = Paint()
                linePaint.color = Color.GREEN
                linePaint.style = Paint.Style.STROKE
                linePaint.strokeWidth = 2f

                for (face in it) {
                    val faceContours = face.getContour(FirebaseVisionFaceContour.FACE)
                        .points
                    for (i in 0 until faceContours.size) {
                        var faceContour: FirebaseVisionPoint? = null

                        if (i != (faceContours.size - 1)) {
                            faceContour = faceContours[i]
                            canvas.drawLine(
                                faceContour.x,
                                faceContour.y,
                                faceContours[i + 1].x,
                                faceContours[i + 1].y,
                                linePaint

                            )

                        } else {
                            canvas.drawLine(
                                faceContour!!.x,
                                faceContour.y,
                                faceContours[0].x,
                                faceContours[0].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(
                            faceContour!!.x,
                            faceContour.y,
                            4f,
                            dotPaint
                        )
                    }
                    val leftEyebrowTopCountours = face.getContour(
                        FirebaseVisionFaceContour.LEFT_EYEBROW_TOP
                    ).points
                    for (i in 0 until leftEyebrowTopCountours.size) {
                        val contour = leftEyebrowTopCountours[i]
                        if (i != (leftEyebrowTopCountours.size - 1))
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                leftEyebrowTopCountours[i + 1].x,
                                leftEyebrowTopCountours[i + 1].y,
                                linePaint
                            )
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val rightEyebrowTopCountours = face.getContour(
                        FirebaseVisionFaceContour.RIGHT_EYEBROW_TOP
                    ).points
                    for (i in 0 until rightEyebrowTopCountours.size) {
                        val contour = rightEyebrowTopCountours[i]
                        if (i != (rightEyebrowTopCountours.size - 1))
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                rightEyebrowTopCountours[i + 1].x,
                                rightEyebrowTopCountours[i + 1].y,
                                linePaint
                            )
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val rightEyebrowBottomCountours = face.getContour(
                        FirebaseVisionFaceContour.RIGHT_EYEBROW_BOTTOM
                    ).points
                    for (i in 0 until rightEyebrowBottomCountours.size) {
                        val contour = rightEyebrowBottomCountours[i]
                        if (i != (rightEyebrowBottomCountours.size - 1))
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                rightEyebrowBottomCountours[i + 1].x,
                                rightEyebrowBottomCountours[i + 1].y,
                                linePaint
                            )
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val leftEyeContours = face.getContour(
                        FirebaseVisionFaceContour.LEFT_EYE
                    ).points
                    for (i in 0 until leftEyeContours.size) {
                        val contour = leftEyeContours[i]
                        if (i != (leftEyeContours.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                leftEyeContours[i + 1].x,
                                leftEyeContours[i + 1].y,
                                linePaint
                            )
                        } else {
                            canvas.drawLine(
                                contour.x, contour.y, leftEyeContours[0].x,
                                leftEyeContours[0].y, linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val rightEyeContours = face.getContour(
                        FirebaseVisionFaceContour.RIGHT_EYE
                    ).points
                    for (i in 0 until rightEyeContours.size) {
                        val contour = rightEyeContours[i]
                        if (i != (rightEyeContours.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                rightEyeContours[i + 1].x,
                                rightEyeContours[i + 1].y,
                                linePaint
                            )
                        } else {
                            canvas.drawLine(
                                contour.x, contour.y, rightEyeContours[0].x,
                                rightEyeContours[0].y, linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val upperLipTopContour = face.getContour(
                        FirebaseVisionFaceContour.UPPER_LIP_TOP
                    ).points
                    for (i in 0 until upperLipTopContour.size) {
                        val contour = upperLipTopContour[i]
                        if (i != (upperLipTopContour.size - 1)) {
                            canvas.drawLine(
                                contour.x, contour.y,
                                upperLipTopContour[i + 1].x,
                                upperLipTopContour[i + 1].y, linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val upperLipBottomContour = face.getContour(
                        FirebaseVisionFaceContour.UPPER_LIP_BOTTOM
                    ).points
                    for (i in 0 until upperLipBottomContour.size) {
                        val contour = upperLipBottomContour[i]
                        if (i != (upperLipBottomContour.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                upperLipBottomContour[i + 1].x,
                                upperLipBottomContour[i + 1].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val lowerLipTopContour = face.getContour(
                        FirebaseVisionFaceContour.LOWER_LIP_TOP
                    ).points
                    for (i in 0 until lowerLipTopContour.size) {
                        val contour = lowerLipTopContour[i]
                        if (i != (lowerLipTopContour.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                lowerLipTopContour[i + 1].x,
                                lowerLipTopContour[i + 1].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val lowerLipBottomContour = face.getContour(
                        FirebaseVisionFaceContour.LOWER_LIP_BOTTOM
                    ).points
                    for (i in 0 until lowerLipBottomContour.size) {
                        val contour = lowerLipBottomContour[i]
                        if (i != (lowerLipBottomContour.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                lowerLipBottomContour[i + 1].x,
                                lowerLipBottomContour[i + 1].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val noseBridgeContours = face.getContour(
                        FirebaseVisionFaceContour.NOSE_BRIDGE
                    ).points
                    for (i in 0 until noseBridgeContours.size) {
                        val contour = noseBridgeContours[i]
                        if (i != (noseBridgeContours.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                noseBridgeContours[i + 1].x,
                                noseBridgeContours[i + 1].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }
                    val noseBottomContours = face.getContour(
                        FirebaseVisionFaceContour.NOSE_BOTTOM
                    ).points
                    for (i in 0 until noseBottomContours.size) {
                        val contour = noseBottomContours[i]
                        if (i != (noseBottomContours.size - 1)) {
                            canvas.drawLine(
                                contour.x,
                                contour.y,
                                noseBottomContours[i + 1].x,
                                noseBottomContours[i + 1].y,
                                linePaint
                            )
                        }
                        canvas.drawCircle(contour.x, contour.y, 4f, dotPaint)
                    }

                }//end forloop


            }

    }


}
