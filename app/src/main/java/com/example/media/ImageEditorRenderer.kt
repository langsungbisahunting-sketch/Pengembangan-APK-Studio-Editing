package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.BlendModePreset
import com.example.model.ImageCropRect
import com.example.model.ImageEditorDocument
import com.example.model.ImageFloodFillOp
import com.example.model.ImageShapeLayer
import com.example.model.PaintBrushType
import com.example.model.PaintShapeType
import com.example.model.TextFontStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

object ImageEditorRenderer {

    fun createBlankCanvasBitmap(
        width: Int = 960,
        height: Int = 960,
        bgColorHex: Long = 0xFFFFFFFFL
    ): Bitmap {
        return Bitmap.createBitmap(width.coerceIn(256, 2048), height.coerceIn(256, 2048), Bitmap.Config.ARGB_8888).apply {
            eraseColor(bgColorHex.toInt())
        }
    }

    suspend fun loadPreviewBitmap(
        context: Context,
        uri: Uri,
        maxDim: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return@withContext null

            var sample = 1
            while (w / sample > maxDim || h / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun samplePixelColor(bitmap: Bitmap, normX: Float, normY: Float): Long {
        val x = (normX * bitmap.width).toInt().coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
        val y = (normY * bitmap.height).toInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        val px = bitmap.getPixel(x, y)
        return px.toLong() and 0xFFFFFFFFL
    }

    /**
     * Iterative BFS flood-fill (Ember Cat MS Paint) safe against StackOverflow on any resolution.
     */
    fun applyFloodFill(
        source: Bitmap,
        normX: Float,
        normY: Float,
        replacementColor: Int,
        tolerance: Int = 38
    ): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 0 || h <= 0) return source
        val startX = (normX * w).toInt().coerceIn(0, w - 1)
        val startY = (normY * h).toInt().coerceIn(0, h - 1)

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val targetColor = pixels[startY * w + startX]
        if (targetColor == replacementColor) {
            return source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val tA = Color.alpha(targetColor)
        val tR = Color.red(targetColor)
        val tG = Color.green(targetColor)
        val tB = Color.blue(targetColor)

        fun matches(px: Int): Boolean {
            return abs(Color.alpha(px) - tA) <= tolerance &&
                abs(Color.red(px) - tR) <= tolerance &&
                abs(Color.green(px) - tG) <= tolerance &&
                abs(Color.blue(px) - tB) <= tolerance
        }

        val visited = BooleanArray(w * h)
        val queue = ArrayDeque<Int>(4096)
        val startIdx = startY * w + startX
        queue.add(startIdx)
        visited[startIdx] = true

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            pixels[idx] = replacementColor
            val x = idx % w
            val y = idx / w

            if (x > 0) {
                val n = idx - 1
                if (!visited[n] && matches(pixels[n])) {
                    visited[n] = true
                    queue.addLast(n)
                }
            }
            if (x < w - 1) {
                val n = idx + 1
                if (!visited[n] && matches(pixels[n])) {
                    visited[n] = true
                    queue.addLast(n)
                }
            }
            if (y > 0) {
                val n = idx - w
                if (!visited[n] && matches(pixels[n])) {
                    visited[n] = true
                    queue.addLast(n)
                }
            }
            if (y < h - 1) {
                val n = idx + w
                if (!visited[n] && matches(pixels[n])) {
                    visited[n] = true
                    queue.addLast(n)
                }
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    fun buildPreviewWorkingBitmap(
        rawBase: Bitmap,
        document: ImageEditorDocument
    ): Bitmap {
        var bmp = transformBitmap(
            raw = rawBase,
            rotationDegrees = document.rotationDegrees,
            flipH = document.flipHorizontal,
            flipV = document.flipVertical,
            appliedCrop = document.appliedCropRect,
            cropRatio = document.cropRatio.ratio
        )
        document.floodFills.forEach { fill: ImageFloodFillOp ->
            bmp = applyFloodFill(
                source = bmp,
                normX = fill.xFraction,
                normY = fill.yFraction,
                replacementColor = fill.colorHex.toInt(),
                tolerance = fill.tolerance
            )
        }
        if (document.removeBackground) {
            bmp = applyBackgroundCutout(bmp, document.bgTolerance)
        }
        return bmp
    }

    /**
     * Heuristic subject/background cutout: samples border pixels to detect dominant backdrop
     * and smoothly fades matching backdrop pixels to transparent (or custom canvas color).
     */
    fun applyBackgroundCutout(source: Bitmap, tolerance: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0L
        val stepX = (w / 16).coerceAtLeast(1)
        val stepY = (h / 16).coerceAtLeast(1)
        for (x in 0 until w step stepX) {
            val top = pixels[x]
            val bot = pixels[(h - 1) * w + x]
            sumR += Color.red(top) + Color.red(bot)
            sumG += Color.green(top) + Color.green(bot)
            sumB += Color.blue(top) + Color.blue(bot)
            count += 2
        }
        for (y in 0 until h step stepY) {
            val left = pixels[y * w]
            val right = pixels[y * w + (w - 1)]
            sumR += Color.red(left) + Color.red(right)
            sumG += Color.green(left) + Color.green(right)
            sumB += Color.blue(left) + Color.blue(right)
            count += 2
        }
        val bgR = (sumR / count.coerceAtLeast(1)).toInt()
        val bgG = (sumG / count.coerceAtLeast(1)).toInt()
        val bgB = (sumB / count.coerceAtLeast(1)).toInt()

        val maxDist = (tolerance.coerceIn(0.05f, 0.7f) * 442f)
        val feather = maxDist * 0.35f

        for (i in pixels.indices) {
            val px = pixels[i]
            val dr = Color.red(px) - bgR
            val dg = Color.green(px) - bgG
            val db = Color.blue(px) - bgB
            val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble()).toFloat()
            if (dist < maxDist) {
                val factor = ((dist - (maxDist - feather)) / feather.coerceAtLeast(1f)).coerceIn(0f, 1f)
                val newAlpha = (Color.alpha(px) * factor).roundToInt().coerceIn(0, 255)
                pixels[i] = Color.argb(newAlpha, Color.red(px), Color.green(px), Color.blue(px))
            }
        }

        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    suspend fun renderDocumentToFile(
        context: Context,
        document: ImageEditorDocument
    ): File? = withContext(Dispatchers.IO) {
        val raw = if (document.sourceUriString != null) {
            loadPreviewBitmap(context, Uri.parse(document.sourceUriString), maxDim = 1440)
        } else {
            createBlankCanvasBitmap(
                width = document.blankCanvasWidth,
                height = document.blankCanvasHeight,
                bgColorHex = document.canvasBaseColorHex
            )
        } ?: return@withContext null

        val baseBitmap = buildPreviewWorkingBitmap(raw, document)

        val outW = baseBitmap.width
        val outH = baseBitmap.height
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        if (document.customBgColorHex != 0x00000000L) {
            canvas.drawColor(document.customBgColorHex.toInt())
        }

        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildAndroidColorMatrix(document))
        }
        canvas.drawBitmap(baseBitmap, 0f, 0f, colorPaint)

        // Vignette overlay
        if (document.adjustments.vignette > 0.02f) {
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    outW / 2f,
                    outH / 2f,
                    max(outW, outH) * 0.65f,
                    intArrayOf(Color.TRANSPARENT, Color.argb((document.adjustments.vignette * 210).roundToInt(), 0, 0, 0)),
                    floatArrayOf(0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), vignettePaint)
        }

        val scaleUnit = min(outW, outH) / 360f

        // Timpa Gambar (Overlay Images)
        document.overlayImages.forEach { overlay ->
            val ovBmp = loadPreviewBitmap(context, Uri.parse(overlay.uriString), maxDim = 960) ?: return@forEach
            canvas.save()
            canvas.translate(overlay.xFraction * outW, overlay.yFraction * outH)
            canvas.rotate(overlay.rotationDeg)
            val targetW = outW * overlay.scale.coerceIn(0.12f, 1.8f)
            val ratio = ovBmp.height.toFloat() / ovBmp.width.coerceAtLeast(1).toFloat()
            val targetH = targetW * ratio
            if (overlay.flipHorizontal) {
                canvas.scale(-1f, 1f)
            }
            val ovPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = (overlay.opacity.coerceIn(0.05f, 1f) * 255).roundToInt()
                val porterDuff = when (overlay.blendMode) {
                    BlendModePreset.MULTIPLY -> PorterDuff.Mode.MULTIPLY
                    BlendModePreset.SCREEN -> PorterDuff.Mode.SCREEN
                    BlendModePreset.OVERLAY -> PorterDuff.Mode.OVERLAY
                    BlendModePreset.LIGHTEN -> PorterDuff.Mode.LIGHTEN
                    BlendModePreset.DARKEN -> PorterDuff.Mode.DARKEN
                    BlendModePreset.NORMAL -> null
                }
                if (porterDuff != null) xfermode = PorterDuffXfermode(porterDuff)
            }
            val dst = RectF(-targetW / 2f, -targetH / 2f, targetW / 2f, targetH / 2f)
            canvas.drawBitmap(ovBmp, null, dst, ovPaint)
            canvas.restore()
        }

        // Paint Strokes (Pencil, Brush, Marker, Airbrush, Eraser)
        val strokeLayer = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val strokeCanvas = Canvas(strokeLayer)
        val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        document.strokes.forEach { stroke ->
            if (stroke.points.size < 2) return@forEach
            if (stroke.isEraser || stroke.brushType == PaintBrushType.ERASER) {
                val path = Path()
                stroke.points.forEachIndexed { idx, pt ->
                    val px = pt.x * outW
                    val py = pt.y * outH
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                brushPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                brushPaint.color = Color.TRANSPARENT
                brushPaint.alpha = 255
                brushPaint.strokeWidth = stroke.strokeWidthDp * scaleUnit
                strokeCanvas.drawPath(path, brushPaint)
            } else if (stroke.brushType == PaintBrushType.AIRBRUSH) {
                val sprayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = stroke.colorHex.toInt()
                    alpha = (stroke.alpha.coerceIn(0.15f, 1f) * 180).roundToInt()
                }
                val rng = Random(stroke.id.hashCode())
                val radius = stroke.strokeWidthDp * scaleUnit * 1.4f
                val dotRadius = (1.2f * scaleUnit).coerceAtLeast(1.5f)
                stroke.points.forEach { pt ->
                    val cx = pt.x * outW
                    val cy = pt.y * outH
                    repeat(14) {
                        val angle = rng.nextFloat() * 6.28318f
                        val dist = rng.nextFloat() * radius
                        strokeCanvas.drawCircle(
                            cx + cos(angle) * dist,
                            cy + sin(angle) * dist,
                            dotRadius,
                            sprayPaint
                        )
                    }
                }
            } else {
                val path = Path()
                stroke.points.forEachIndexed { idx, pt ->
                    val px = pt.x * outW
                    val py = pt.y * outH
                    if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                brushPaint.xfermode = null
                brushPaint.color = stroke.colorHex.toInt()
                val effAlpha = when (stroke.brushType) {
                    PaintBrushType.MARKER -> (stroke.alpha * 0.45f).coerceIn(0.12f, 0.7f)
                    else -> stroke.alpha.coerceIn(0.1f, 1f)
                }
                brushPaint.alpha = (effAlpha * 255).roundToInt()
                val widthFactor = when (stroke.brushType) {
                    PaintBrushType.PENCIL -> 0.42f
                    PaintBrushType.MARKER -> 1.7f
                    else -> 1f
                }
                brushPaint.strokeWidth = (stroke.strokeWidthDp * widthFactor * scaleUnit).coerceAtLeast(1.5f)
                strokeCanvas.drawPath(path, brushPaint)
            }
        }
        canvas.drawBitmap(strokeLayer, 0f, 0f, null)

        // MS Paint Geometric Shapes
        document.shapes.forEach { shape ->
            drawShapeOnAndroidCanvas(canvas, shape, outW.toFloat(), outH.toFloat(), scaleUnit)
        }

        // Stickers & Comic Bubbles
        document.stickers.forEach { st ->
            canvas.save()
            canvas.translate(st.xFraction * outW, st.yFraction * outH)
            canvas.rotate(st.rotationDeg)
            canvas.scale(st.scale, st.scale)
            val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = st.fgHex.toInt()
                textSize = 26f * scaleUnit
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val tw = txtPaint.measureText(st.label)
            val padX = 18f * scaleUnit
            val padY = 12f * scaleUnit
            val rect = RectF(-tw / 2f - padX, -txtPaint.textSize - padY * 0.5f, tw / 2f + padX, padY)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.bgHex.toInt() }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * scaleUnit
                color = Color.BLACK
            }
            canvas.drawRoundRect(rect, 18f * scaleUnit, 18f * scaleUnit, bgPaint)
            canvas.drawRoundRect(rect, 18f * scaleUnit, 18f * scaleUnit, borderPaint)
            canvas.drawText(st.label, 0f, -padY * 0.25f, txtPaint)
            canvas.restore()
        }

        // Text Layers
        document.textLayers.forEach { layer ->
            canvas.save()
            canvas.translate(layer.xFraction * outW, layer.yFraction * outH)
            canvas.rotate(layer.rotationDeg)
            canvas.scale(layer.scale, layer.scale)

            val tf = when (layer.fontStyle) {
                TextFontStyle.MODERN -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                TextFontStyle.CLEAN -> Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                TextFontStyle.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = layer.colorHex.toInt()
                textSize = 28f * scaleUnit
                typeface = tf
                textAlign = Paint.Align.CENTER
                if (layer.hasShadow) {
                    setShadowLayer(6f * scaleUnit, 2f * scaleUnit, 2f * scaleUnit, Color.argb(200, 0, 0, 0))
                }
            }
            val tw = textPaint.measureText(layer.text)
            val padX = 16f * scaleUnit
            val padY = 10f * scaleUnit
            if (layer.useBubble && Color.alpha(layer.bgColorHex.toInt()) > 0) {
                val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.bgColorHex.toInt() }
                val boxRect = RectF(-tw / 2f - padX, -textPaint.textSize - padY * 0.4f, tw / 2f + padX, padY)
                canvas.drawRoundRect(boxRect, 14f * scaleUnit, 14f * scaleUnit, boxPaint)
            }
            if (layer.strokeEnabled) {
                val strokePaint = Paint(textPaint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3.5f * scaleUnit
                    color = Color.BLACK
                    clearShadowLayer()
                }
                canvas.drawText(layer.text, 0f, -padY * 0.1f, strokePaint)
            }
            canvas.drawText(layer.text, 0f, -padY * 0.1f, textPaint)
            canvas.restore()
        }

        val dir = File(context.filesDir, "edited_images").apply { mkdirs() }
        val outFile = File(dir, "KN_IMG_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            result.compress(Bitmap.CompressFormat.PNG, 95, fos)
        }
        outFile
    }

    private fun drawShapeOnAndroidCanvas(
        canvas: Canvas,
        shape: ImageShapeLayer,
        w: Float,
        h: Float,
        scaleUnit: Float
    ) {
        val sx = shape.startX * w
        val sy = shape.startY * h
        val ex = shape.endX * w
        val ey = shape.endY * h
        val left = min(sx, ex)
        val top = min(sy, ey)
        val right = max(sx, ex)
        val bottom = max(sy, ey)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shape.colorHex.toInt()
            alpha = (shape.alpha.coerceIn(0.1f, 1f) * 255).roundToInt()
            strokeWidth = shape.strokeWidthDp * scaleUnit
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = if (shape.filled && shape.shapeType != PaintShapeType.LINE && shape.shapeType != PaintShapeType.ARROW) {
                Paint.Style.FILL
            } else {
                Paint.Style.STROKE
            }
        }

        when (shape.shapeType) {
            PaintShapeType.LINE -> canvas.drawLine(sx, sy, ex, ey, paint)
            PaintShapeType.ARROW -> {
                canvas.drawLine(sx, sy, ex, ey, paint)
                val angle = atan2((ey - sy).toDouble(), (ex - sx).toDouble())
                val headLen = (16f + shape.strokeWidthDp * 1.5f) * scaleUnit
                val a1 = angle - Math.PI / 6.0
                val a2 = angle + Math.PI / 6.0
                canvas.drawLine(ex, ey, ex - (headLen * cos(a1)).toFloat(), ey - (headLen * sin(a1)).toFloat(), paint)
                canvas.drawLine(ex, ey, ex - (headLen * cos(a2)).toFloat(), ey - (headLen * sin(a2)).toFloat(), paint)
            }
            PaintShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, paint)
            PaintShapeType.OVAL -> canvas.drawOval(RectF(left, top, right, bottom), paint)
            PaintShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo((left + right) / 2f, top)
                    lineTo(right, bottom)
                    lineTo(left, bottom)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            PaintShapeType.STAR -> {
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val outerR = max(right - left, bottom - top) / 2f
                val innerR = outerR * 0.42f
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val rad = Math.toRadians(-90.0 + i * 36.0)
                    val px = cx + (r * cos(rad)).toFloat()
                    val py = cy + (r * sin(rad)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
            PaintShapeType.BUBBLE -> {
                val bodyBottom = bottom - (bottom - top) * 0.22f
                val rect = RectF(left, top, right, bodyBottom.coerceAtLeast(top + 8f))
                canvas.drawRoundRect(rect, 18f * scaleUnit, 18f * scaleUnit, paint)
                val tail = Path().apply {
                    moveTo(left + (right - left) * 0.25f, bodyBottom)
                    lineTo(left + (right - left) * 0.14f, bottom)
                    lineTo(left + (right - left) * 0.42f, bodyBottom)
                    close()
                }
                canvas.drawPath(tail, paint)
            }
        }
    }

    suspend fun saveImageToGallery(context: Context, imageFile: File, title: String): Uri? =
        withContext(Dispatchers.IO) {
            if (!imageFile.exists()) return@withContext null
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$title.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KomikNarasi")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = resolver.insert(collection, values) ?: return@withContext null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    imageFile.inputStream().use { input -> input.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                uri
            } catch (_: Throwable) {
                resolver.delete(uri, null, null)
                null
            }
        }

    private fun transformBitmap(
        raw: Bitmap,
        rotationDegrees: Int,
        flipH: Boolean,
        flipV: Boolean,
        appliedCrop: ImageCropRect,
        cropRatio: Float?
    ): Bitmap {
        val m = Matrix().apply {
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
            postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        }
        val rotated = if (rotationDegrees != 0 || flipH || flipV) {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
        } else raw

        val croppedFree = if (!appliedCrop.isFull) {
            val cLeft = (appliedCrop.left * rotated.width).roundToInt().coerceIn(0, (rotated.width - 10).coerceAtLeast(0))
            val cTop = (appliedCrop.top * rotated.height).roundToInt().coerceIn(0, (rotated.height - 10).coerceAtLeast(0))
            val cRight = (appliedCrop.right * rotated.width).roundToInt().coerceIn(cLeft + 8, rotated.width)
            val cBottom = (appliedCrop.bottom * rotated.height).roundToInt().coerceIn(cTop + 8, rotated.height)
            Bitmap.createBitmap(rotated, cLeft, cTop, (cRight - cLeft).coerceAtLeast(1), (cBottom - cTop).coerceAtLeast(1))
        } else rotated

        if (cropRatio == null || cropRatio <= 0f) return croppedFree

        val w = croppedFree.width
        val h = croppedFree.height
        val currentRatio = w.toFloat() / h.toFloat()
        if (abs(currentRatio - cropRatio) < 0.01f) return croppedFree

        val (targetW, targetH) = if (currentRatio > cropRatio) {
            (h * cropRatio).roundToInt().coerceIn(1, w) to h
        } else {
            w to (w / cropRatio).roundToInt().coerceIn(1, h)
        }
        val left = ((w - targetW) / 2).coerceAtLeast(0)
        val top = ((h - targetH) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(croppedFree, left, top, targetW, targetH)
    }

    private fun buildAndroidColorMatrix(doc: ImageEditorDocument): ColorMatrix {
        val f = doc.filterPreset
        val adj = doc.adjustments
        val sat = if (f.isGrayscale) 0f else (1f + f.saturation + adj.saturation).coerceIn(0f, 2f)
        val contrast = (1f + f.contrast + adj.contrast + adj.sharpen * 0.14f).coerceIn(0.3f, 2.2f)
        val bright = ((f.brightness + adj.brightness + adj.exposure * 0.85f) * 255f).coerceIn(-160f, 160f)

        val satMatrix = ColorMatrix().apply { setSaturation(sat) }
        val translate = (1f - contrast) * 127.5f + bright
        val toneMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(toneMatrix)
        return satMatrix
    }
}
