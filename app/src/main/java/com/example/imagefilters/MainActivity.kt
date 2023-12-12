package com.example.imagefilters

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.imagefilters.ui.theme.ImageFiltersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize OpenCV
        // if (!OpenCVLoader.initDebug()) {
        //     Log.e("OpenCV", "Unable to load OpenCV!")
        // } else {
        //     Log.d("OpenCV", "OpenCV loaded successfully!")
        //     // Your existing code...
        // }

        setContent {
            ImageFiltersTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImagePickerAndDisplay() // Our custom composable for image picking
                }
            }
        }
    }
}