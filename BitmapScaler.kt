package com.example.finalproject.variables

import android.graphics.Bitmap

object BitmapScaler {
    // Scale and maintain aspect ratio
    // BitmapScaler.scaleToFitWidth(bitmap, 100);

    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val dimensions = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * dimensions).toInt(), true)
    }

    // Scale and maintain aspect ratio
    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val dimensions = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, height, (b.width * dimensions).toInt(), true)
    }
}