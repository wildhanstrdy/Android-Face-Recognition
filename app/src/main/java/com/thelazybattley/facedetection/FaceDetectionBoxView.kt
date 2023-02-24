package com.thelazybattley.facedetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class FaceDetectionBoxView(context: Context, attr: AttributeSet?) : View(context, attr) {

    private var rect: Rect? = null

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (rect != null) {
            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 10f
            canvas?.drawRect(rect!!, paint)
        }
    }

    fun setRect(rect: Rect) {
        this.rect = rect
        invalidate()
    }

}
