package com.example.smartcoaster.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * V2：將原 HTML 的青綠色水滴 + 編號移植成 Android Google Maps 自訂 Marker。
 * 使用 Canvas 即時繪製，不需額外圖片或第三方圖示 API。
 */
internal object WaterDropMarkerFactory {
    private val normal = Color.rgb(18, 159, 146)
    private val hot = Color.rgb(229, 57, 53)
    private val icedWarm = Color.rgb(251, 140, 0)
    private val blue = Color.rgb(22, 133, 229)

    fun create(number: Int, kind: String, online: Boolean = false, selected: Boolean = false): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(144, 170, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val color = when (kind) {
            "熱水", "溫水" -> hot
            "冰溫" -> icedWarm
            else -> normal
        }
        // 水滴尖端朝下，Marker Anchor = (0.5, 1.0)，位置精準對齊地圖座標。
        val outline = Path().apply {
            moveTo(72f, 156f)
            cubicTo(57f, 135f, 21f, 98f, 21f, 72f)
            cubicTo(21f, 42f, 44f, 20f, 72f, 20f)
            cubicTo(100f, 20f, 123f, 42f, 123f, 72f)
            cubicTo(123f, 98f, 87f, 135f, 72f, 156f)
            close()
        }
        p.style = Paint.Style.FILL
        p.color = Color.argb(55, 0, 0, 0)
        c.save()
        c.translate(0f, 5f)
        c.drawPath(outline, p)
        c.restore()
        p.color = Color.WHITE
        c.drawPath(outline, p)
        c.save()
        c.scale(0.90f, 0.90f, 72f, 75f)
        p.color = color
        c.drawPath(outline, p)
        c.restore()
        // 水滴亮面與文字使用白色；編號越大自動調整大小。
        p.color = Color.argb(90, 255, 255, 255)
        c.drawOval(RectF(44f, 36f, 60f, 75f), p)
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = if (number >= 100) 30f else if (number >= 10) 37f else 44f
        c.drawText(number.toString(), 77f, 92f, p)
        if (online) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.color = Color.rgb(22, 138, 85)
            c.drawCircle(72f, 70f, 59f, p)
            p.style = Paint.Style.FILL
            p.color = Color.rgb(22, 138, 85)
            c.drawRoundRect(RectF(89f, 3f, 142f, 29f), 10f, 10f, p)
            p.color = Color.WHITE
            p.textSize = 17f
            c.drawText("NEW", 115f, 22f, p)
        }
        if (selected) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 6f
            p.color = blue
            c.drawCircle(72f, 70f, 62f, p)
            p.style = Paint.Style.FILL
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun searchCenter(): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = blue }
        c.drawCircle(48f, 48f, 43f, p)
        p.color = Color.WHITE
        c.drawCircle(48f, 48f, 34f, p)
        p.color = blue
        c.drawCircle(48f, 48f, 22f, p)
        p.color = Color.WHITE
        c.drawCircle(48f, 48f, 9f, p)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
