package com.example.thuhuong_restaurant.core.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Chữ viết tay cần độ phân giải cao hơn ảnh thường để đọc được nét mảnh. */
private const val MAX_DIMENSION = 2200
private const val JPEG_QUALITY = 90

/**
 * Loads [uri], corrects EXIF orientation, downscales, boosts contrast for handwriting, and returns
 * a base64 JPEG.
 *
 * Decoding is two-pass (bounds first, then `inSampleSize`) so a 12MP photo never materializes at
 * full size in heap — the naive single-pass decode was an OOM risk on low-end phones.
 */
fun Context.uriToScaledJpegBase64(uri: Uri): String? {
    val bitmap = decodeScaled(uri, MAX_DIMENSION) ?: return null
    val rotated = correctOrientation(uri, bitmap)
    val scaled = downscale(rotated, MAX_DIMENSION)
    val enhanced = boostContrast(scaled)
    val out = ByteArrayOutputStream()
    enhanced.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** Reads only the bounds first so the full-size bitmap is never allocated. */
private fun Context.decodeScaled(uri: Uri, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    var longSide = maxOf(bounds.outWidth, bounds.outHeight)
    // Halve until just above the target — decoding a bit large then scaling keeps quality
    while (longSide / 2 >= maxDimension) {
        sample *= 2
        longSide /= 2
    }

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

private fun Context.correctOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
    val matrix = try {
        contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
                ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(270f) }
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1f, -1f) }
                ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply { postRotate(270f); postScale(-1f, 1f) }
                else -> null
            }
        }
    } catch (e: Exception) {
        null
    } ?: return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val longSide = maxOf(bitmap.width, bitmap.height)
    if (longSide <= maxDimension) return bitmap
    val scale = maxDimension.toFloat() / longSide
    val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

/**
 * Desaturates and stretches contrast so pen strokes separate from the paper. Ballpoint on white
 * paper under restaurant lighting is often low-contrast, which is exactly what trips up OCR.
 * Uses ColorMatrix only — no extra image library needed.
 */
private fun boostContrast(source: Bitmap): Bitmap {
    val contrast = 1.6f
    // Keep mid-grey fixed while expanding the range around it
    val shift = (-0.5f * contrast + 0.5f) * 255f
    val matrix = ColorMatrix().apply {
        setSaturation(0f)
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, shift,
                    0f, contrast, 0f, 0f, shift,
                    0f, 0f, contrast, 0f, shift,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }
    val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(source, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
    return out
}

/** Fresh cache file under `cacheDir/receipts/` + its FileProvider content:// Uri, for `TakePicture()`. */
fun Context.createReceiptImageUri(): Uri {
    val dir = File(cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}
