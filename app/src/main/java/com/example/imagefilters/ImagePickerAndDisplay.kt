package com.example.imagefilters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max

@Composable
fun ImagePickerAndDisplay() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var blurredImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurRadius by remember { mutableStateOf(1f) }
    val context = LocalContext.current
    val blurCache = remember { mutableMapOf<String, Bitmap?>() }
    var selectedBlurType by remember { mutableStateOf("gaussian") }
    var showDropdown by remember { mutableStateOf(false) }
    val blurTypes = listOf("gaussian", "motion", "pixelate", "zoom", "box")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        blurredImageBitmap = null
        uri?.let {
            val cacheKey = "$blurRadius-$selectedBlurType"
            blurredImageBitmap =
                blurCache[cacheKey] ?: applyBlur(context, uri, blurRadius, selectedBlurType).also {
                    blurCache[cacheKey] = it
                }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            blurTypes.forEach { blurType ->
                Button(
                    onClick = { selectedBlurType = blurType },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(blurType)
                }
            }
        }

        Button(
            onClick = { launcher.launch("image/*") }
        ) {
            Text(text = "Upload Image")
        }

        Slider(
            value = blurRadius,
            onValueChange = { newValue ->
                blurRadius = newValue
                imageUri?.let {
                    val cacheKey = "$blurRadius-$selectedBlurType"
                    blurredImageBitmap = blurCache[cacheKey] ?: applyBlur(
                        context,
                        it,
                        blurRadius,
                        selectedBlurType
                    ).also {
                        blurCache[cacheKey] = it
                    }
                }
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.padding(16.dp)
        )

        blurredImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Blurred Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}


private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val ratio = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * ratio).toInt(),
        (bitmap.height * ratio).toInt(),
        true
    )
}
private fun applyBlur(context: Context, imageUri: Uri, blurLevel: Float, blurType: String): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    if (bitmap == null) {
        Log.e("ImageBlur", "Failed to decode bitmap from URI")
        return null
    }

    val correctedBitmap = correctImageOrientation(context, imageUri, bitmap)

    val srcMat = Mat()
    Utils.bitmapToMat(correctedBitmap, srcMat)
    val blurredMat = when (blurType) {
        "gaussian" -> applyGaussianBlur(srcMat, blurLevel)
        "motion" -> applyMotionBlur(srcMat, blurLevel.toInt())
        "pixelate" -> applyPixelate(srcMat, blurLevel.toInt())
        "zoom" -> applyZoomBlur(srcMat, blurLevel.toDouble())
        "box" -> applyBoxBlur(srcMat, blurLevel.toInt())
        else -> srcMat // default or unknown type
    }

    val blurredBitmap =
        Bitmap.createBitmap(blurredMat.cols(), blurredMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(blurredMat, blurredBitmap)

    srcMat.release()
    blurredMat.release()

    Log.d("ImageBlur", "Blur applied with level: $blurLevel, type: $blurType")
    return blurredBitmap
}

private fun applyGaussianBlur(srcMat: Mat, blurLevel: Float): Mat {
    val dstMat = Mat()
    Imgproc.GaussianBlur(
        srcMat,
        dstMat,
        Size((blurLevel * 2 + 1).toDouble(), (blurLevel * 2 + 1).toDouble()),
        0.0
    )
    return dstMat
}

private fun applyMotionBlur(srcMat: Mat, blurSize: Int): Mat {
    val kernelSize = blurSize / 2
    val kernel = Mat.zeros(Size(blurSize.toDouble(), blurSize.toDouble()), CvType.CV_32F)
    for (i in 0 until blurSize) {
        kernel.put(kernelSize, i, 1.0 / blurSize)
    }

    val dstMat = Mat()
    Imgproc.filter2D(srcMat, dstMat, -1, kernel)
    kernel.release()
    return dstMat
}

private fun applyPixelate(srcMat: Mat, pixelSize: Int): Mat {
    val dstMat = Mat()
    Imgproc.resize(
        srcMat,
        dstMat,
        Size(),
        1.0 / pixelSize,
        1.0 / pixelSize,
        Imgproc.INTER_LINEAR
    )
    Imgproc.resize(dstMat, dstMat, srcMat.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)
    return dstMat
}

private fun applyZoomBlur(srcMat: Mat, blurStrength: Double): Mat {
    val dstMat = Mat()
    Imgproc.resize(
        srcMat,
        dstMat,
        Size(),
        1 - blurStrength,
        1 - blurStrength,
        Imgproc.INTER_LINEAR
    )
    Imgproc.resize(dstMat, dstMat, srcMat.size(), 0.0, 0.0, Imgproc.INTER_LINEAR)
    Core.addWeighted(srcMat, 1.0 - blurStrength, dstMat, blurStrength, 0.0, dstMat)
    return dstMat
}

private fun applyBoxBlur(srcMat: Mat, blurSize: Int): Mat {
    val dstMat = Mat()
    Imgproc.blur(srcMat, dstMat, Size(blurSize.toDouble(), blurSize.toDouble()))
    return dstMat
}
