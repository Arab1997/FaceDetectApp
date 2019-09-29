package com.prattham.facedetectapp.model

class FaceDetectionModel {
    var id: Int = 0
    var text: String? = null


    constructor(id: Int, text: String) {
        this.id = id
        this.text = text
    }
}
