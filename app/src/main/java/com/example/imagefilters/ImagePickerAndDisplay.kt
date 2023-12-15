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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import applyBoxBlur
import applyGaussianBlur
import applyHueEffect
import applyMotionBlur
import applyPixelate
import applySepiaFilter
import applyTransferEffect
import applyVibranceEffect
import applyZoomBlur
import org.opencv.android.Utils
import org.opencv.core.Mat
import kotlin.math.max

@Composable
fun ImagePickerAndDisplay() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var blurredImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurRadius by remember { mutableStateOf(1f) }
    val context = LocalContext.current
    val blurCache = remember { mutableMapOf<String, Bitmap?>() }
    var selectedBlurType by remember { mutableStateOf("gaussian") }
    val blurTypes = listOf(
        "gaussian",
        "motion",
        "pixelate",
        "zoom",
        "box",
        "sepia",
        "hue",
        "vibrance",
        "transfer"
    )
    var processedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        blurredImageBitmap = null
        uri?.let {
            processedImageBitmap = processImageBeforeBlurring(context, it)
            processedImageBitmap?.let { bitmap ->
                val cacheKey = "$blurRadius-$selectedBlurType"
                blurredImageBitmap = blurCache[cacheKey] ?: applyBlur(
                    bitmap, blurRadius, selectedBlurType
                ).also {
                    blurCache[cacheKey] = it
                }
            }
        }
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOptionText by remember { mutableStateOf(blurTypes.first()) }

        Column {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedOptionText)
                Icon(Icons.Filled.ArrowDropDown, "contentDescription")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                blurTypes.forEach { label ->
                    DropdownMenuItem(onClick = {
                        expanded = false
                        selectedOptionText = label
                        selectedBlurType = label
                    }, text = { Text(text = label) })
                }
            }
        }


        Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Upload Image")
        }

        Slider(
            value = blurRadius, onValueChange = { newValue ->
                blurRadius = newValue
                processedImageBitmap?.let { bitmap ->
                    val cacheKey = "$blurRadius-$selectedBlurType"
                    blurredImageBitmap = blurCache[cacheKey] ?: applyBlur(
                        bitmap, blurRadius, selectedBlurType
                    ).also {
                        blurCache[cacheKey] = it
                    }
                }
            }, valueRange = 1f..10f, steps = 8, modifier = Modifier.padding(16.dp)
        )

        blurredImageBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Blurred Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

private fun processImageBeforeBlurring(context: Context, imageUri: Uri): Bitmap? {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    if (bitmap == null) {
        Log.e("ImageBlur", "Failed to decode bitmap from URI")
        return null
    }

    val correctedBitmap = correctImageOrientation(context, imageUri, bitmap)
    return scaleDownBitmap(correctedBitmap, maxDimension = 1024) // Scale down the bitmap
}

private fun scaleDownBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val ratio = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
    return Bitmap.createScaledBitmap(
        bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true
    )
}

private fun applyBlur(
    bitmap: Bitmap, blurLevel: Float, blurType: String
): Bitmap {
    val srcMat = Mat()
    Utils.bitmapToMat(bitmap, srcMat)
    Log.d("Blur", "Applying $blurType blur with level $blurLevel")
    val blurredMat = when (blurType) {
        "gaussian" -> applyGaussianBlur(srcMat, blurLevel)
        "motion" -> applyMotionBlur(srcMat, blurLevel.toInt())
        "pixelate" -> applyPixelate(srcMat, blurLevel.toInt())
        "zoom" -> applyZoomBlur(srcMat, blurLevel.toDouble())
        "box" -> applyBoxBlur(srcMat, blurLevel.toInt())
        "sepia" -> applySepiaFilter(srcMat, blurLevel.toDouble() / 10.0)
        "hue" -> applyHueEffect(srcMat, blurLevel.toDouble())
        "vibrance" -> applyVibranceEffect(srcMat, blurLevel / 10.0f)
        "transfer" -> applyTransferEffect(srcMat)
        else -> srcMat // default or unknown type
    }

    val blurredBitmap =
        Bitmap.createBitmap(blurredMat.cols(), blurredMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(blurredMat, blurredBitmap)

    srcMat.release()
    blurredMat.release()

    return blurredBitmap
}