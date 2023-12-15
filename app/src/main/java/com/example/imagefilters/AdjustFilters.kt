import android.util.Log
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.exp
import kotlin.math.pow

fun applySepiaFilter(srcMat: Mat, strength: Double): Mat {
    // Sepia tone matrix
    val sepiaMatrix = Mat(4, 4, CvType.CV_32F)
    sepiaMatrix.put(0, 0, /* R */ 0.393, 0.769, 0.189, 0.0)
    sepiaMatrix.put(1, 0, /* G */ 0.349, 0.686, 0.168, 0.0)
    sepiaMatrix.put(2, 0, /* B */ 0.272, 0.534, 0.131, 0.0)
    sepiaMatrix.put(3, 0, /* A */ 0.0, 0.0, 0.0, 1.0)

    // Apply sepia matrix
    val sepiaMat = Mat()
    Core.transform(srcMat, sepiaMat, sepiaMatrix)

    // Blend the original and sepia images based on strength
    val outputMat = Mat()
    Log.d("Sepia", "Strength: $strength")
    Core.addWeighted(srcMat, 1 - strength, sepiaMat, strength, 0.0, outputMat)

    sepiaMatrix.release()
    sepiaMat.release()

    return outputMat
}

private fun createGaussianKernel(length: Int, sigma: Double): DoubleArray {
    val kernel = DoubleArray(length)
    val mid = length / 2
    val sigma22 = 2 * sigma.pow(2)
    val sigma22sqrtPi = sigma22 * kotlin.math.PI
    var sum = 0.0

    for (i in 0 until length) {
        val x = (i - mid).toDouble()
        kernel[i] = exp(-(x * x) / sigma22) / sigma22sqrtPi
        sum += kernel[i]
    }

    // Normalize
    for (i in 0 until length) {
        kernel[i] /= sum
    }

    return kernel
}

fun applyHueEffect(srcMat: Mat, hueValue: Double): Mat {
    val hsvMat = Mat()
    Imgproc.cvtColor(srcMat, hsvMat, Imgproc.COLOR_BGR2HSV)

    val channels = ArrayList<Mat>()
    Core.split(hsvMat, channels)
    Core.add(channels[0], Scalar(hueValue), channels[0]) // Adjust Hue

    Core.merge(channels, hsvMat)
    val dstMat = Mat()
    Imgproc.cvtColor(hsvMat, dstMat, Imgproc.COLOR_HSV2BGR)

    hsvMat.release()
    channels.forEach { it.release() }

    return dstMat
}

fun applyVibranceEffect(inputImage: Mat, vibranceFactor: Float): Mat {
    // Convert to HSV color space
    val hsvImage = Mat()
    Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV)

    // Split the channels
    val channels = ArrayList<Mat>()
    Core.split(hsvImage, channels)

    // Enhance the saturation channel
    val saturation = channels[1]
    enhanceSaturation(saturation, vibranceFactor)

    // Merge back the channels and convert to BGR
    Core.merge(channels, hsvImage)
    val outputImage = Mat()
    Imgproc.cvtColor(hsvImage, outputImage, Imgproc.COLOR_HSV2BGR)

    // Release resources
    hsvImage.release()
    saturation.release()
    channels.forEach { it.release() }

    return outputImage
}

private fun enhanceSaturation(saturation: Mat, vibranceFactor: Float) {
    val width = saturation.cols()
    val height = saturation.rows()
    val buffer = ByteArray(width * height)
    saturation.get(0, 0, buffer)

    for (i in buffer.indices) {
        // Convert byte to unsigned int for processing
        var satVal = buffer[i].toInt() and 0xFF
        val increase = (1 - Math.pow(
            (satVal / 255.0),
            2.5
        )) * vibranceFactor * 255.0 // Adjust the power for different effects

        satVal += increase.toInt()
        satVal = satVal.coerceIn(0, 255)

        buffer[i] = satVal.toByte()
    }

    saturation.put(0, 0, buffer)
}

fun applyTransferEffect(inputImage: Mat): Mat {
    // Convert to HSV color space
    val hsvImage = Mat()
    Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV)

    // Split the channels
    val channels = ArrayList<Mat>()
    Core.split(hsvImage, channels)

    // Adjust the saturation (channel 1) - Reduce it a bit
    val saturation = channels[1]
    adjustSaturation(saturation, 0.85f)  // Reduce saturation to 85%

    // Adjust the V (Value) channel (channel 2) for contrast
    val valueChannel = channels[2]
    increaseContrast(valueChannel, 1.3)  // Adjust contrast factor as needed

    // Merge back the channels and convert to BGR
    Core.merge(channels, hsvImage)
    val outputImage = Mat()
    Imgproc.cvtColor(hsvImage, outputImage, Imgproc.COLOR_HSV2BGR)

    // Release resources
    hsvImage.release()
    saturation.release()
    valueChannel.release()
    channels.forEach { it.release() }

    return outputImage
}

private fun adjustSaturation(saturation: Mat, saturationFactor: Float) {
    // Adjust the saturation of the saturation channel
    Core.multiply(saturation, Scalar(saturationFactor.toDouble()), saturation)
}

private fun increaseContrast(valueChannel: Mat, contrastFactor: Double) {
    // Adjust the contrast of the value channel
    valueChannel.convertTo(valueChannel, -1, contrastFactor, 0.0)
}
