package com.example.imagefilters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGaussianBlurFilter
import kotlinx.coroutines.Job
import kotlin.math.max

private var debounceJob: Job? = null

@Composable
fun ImagePickerAndDisplay() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var blurredImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var blurRadius by remember { mutableStateOf(1f) } // Blur level now goes from 1 to 10
    val context = LocalContext.current
    val blurCache = remember { mutableMapOf<Float, Bitmap?>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri // Handle the returned Uri
        blurredImageBitmap = null
        uri?.let {
            blurredImageBitmap = blurCache[blurRadius] ?: applyBlur(context, uri, blurRadius).also {
                blurCache[blurRadius] = it // Cache the result
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                // Launch the image picker
                launcher.launch("image/*")
            }
        ) {
            Text(text = blurRadius.toString())
        }

        // Slider to adjust blur radius
        val scope = rememberCoroutineScope()

        Slider(
            value = blurRadius,
            onValueChange = { newValue ->
                blurRadius = newValue
                imageUri?.let {
                    blurredImageBitmap = blurCache[blurRadius] ?: applyBlur(context, it, blurRadius).also {
                        blurCache[blurRadius] = it // Cache the result
                    }
                }
            },
            valueRange = 0f..9f,
            steps = 8, // 10 steps (1-10)
            modifier = Modifier.padding(16.dp)
        )


        // Display the image
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

private fun applyBlur(context: Context, imageUri: Uri, blurRadius: Float): Bitmap {
    val inputStream = context.contentResolver.openInputStream(imageUri)
    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    val scaledBitmap = scaleDownBitmap(originalBitmap, 1024) // Example max dimension
    val correctedBitmap = correctImageOrientation(context, imageUri, scaledBitmap)

    val gpuImage = GPUImage(context).apply {
        setImage(correctedBitmap)
        setFilter(GPUImageGaussianBlurFilter().apply {
            setBlurSize(blurRadius)
        })
    }
    return gpuImage.bitmapWithFilterApplied
}