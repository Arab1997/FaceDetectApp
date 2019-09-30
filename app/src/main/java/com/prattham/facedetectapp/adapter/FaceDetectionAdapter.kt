package com.prattham.facedetectapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.prattham.facedetectapp.R
import com.prattham.facedetectapp.model.FaceDetectionModel
import kotlinx.android.synthetic.main.item_face_detection.view.*

class FaceDetectionAdapter(
    private val faceDetectionModelList: List<FaceDetectionModel>,
    private val context: Context
) : RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.item_face_detection, parent, false)
        return ViewHolder(view)

    }

    override fun getItemCount(): Int {
        return faceDetectionModelList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val faceDetectionModel = faceDetectionModelList[position]
        holder.itemView.item_face_detection_text_view1.text = faceDetectionModel.id.toString()
        holder.itemView.item_face_detection_text_view2.text = faceDetectionModel.text
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


}
