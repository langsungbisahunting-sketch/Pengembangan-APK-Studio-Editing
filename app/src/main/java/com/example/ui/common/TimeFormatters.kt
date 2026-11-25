package com.example.ui.common

import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.ColorAdjustments
import com.example.model.FilterPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimecodeMs(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val tenths = (safeMs % 1000L) / 100L
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}

fun formatDurationShort(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

fun formatTimestamp(timestampMs: Long): String {
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

fun buildComposeColorMatrix(
    filter: FilterPreset,
    adjustments: ColorAdjustments
): ColorMatrix {
    val matrix = ColorMatrix()
    val sat = if (filter.isGrayscale) 0f else (1f + filter.saturation + adjustments.saturation).coerceIn(0f, 2f)
    matrix.setToSaturation(sat)

    val contrast = (1f + filter.contrast + adjustments.contrast).coerceIn(0.2f, 2.2f)
    val translateContrast = (-0.5f * contrast + 0.5f) * 255f
    val brightnessOffset = (filter.brightness + adjustments.brightness).coerceIn(-0.6f, 0.6f) * 255f

    val bcMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translateContrast + brightnessOffset,
            0f, contrast, 0f, 0f, translateContrast + brightnessOffset,
            0f, 0f, contrast, 0f, translateContrast + brightnessOffset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    matrix.timesAssign(bcMatrix)
    return matrix
}
