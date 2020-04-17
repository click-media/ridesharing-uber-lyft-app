package com.mindorks.ridesharing.utils

import android.content.Context
import android.graphics.*
import com.mindorks.ridesharing.R

object MapUtils {
    fun getCarBitmap(context: Context): Bitmap{
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return  Bitmap.createScaledBitmap(bitmap, 50, 100, false)
    }

    fun getDestinationBitmap(): Bitmap{
        val height = 20
        val width = 20
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.run{
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }
}