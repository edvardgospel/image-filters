import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

 fun applyGaussianBlur(srcMat: Mat, blurLevel: Float): Mat {
    val dstMat = Mat()
    Imgproc.GaussianBlur(
        srcMat,
        dstMat,
        Size((blurLevel * 2 + 1).toDouble(), (blurLevel * 2 + 1).toDouble()),
        0.0
    )
    return dstMat
}

 fun applyMotionBlur(srcMat: Mat, blurSize: Int): Mat {
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

 fun applyPixelate(srcMat: Mat, pixelSize: Int): Mat {
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

 fun applyZoomBlur(srcMat: Mat, blurStrength: Double): Mat {
    val scaleFactor = 0.5 // Reduce this to increase performance
    val downscaledSize = Size(srcMat.cols() * scaleFactor, srcMat.rows() * scaleFactor)

    // Downscale
    val downscaledMat = Mat()
    Imgproc.resize(srcMat, downscaledMat, downscaledSize)

    // Apply blur on downscaled image
    val blurredMat = Mat()
    Imgproc.GaussianBlur(downscaledMat, blurredMat, Size(), blurStrength)

    // Upscale back to original size
    val upscaledMat = Mat()
    Imgproc.resize(blurredMat, upscaledMat, srcMat.size())

    downscaledMat.release()
    blurredMat.release()

    return upscaledMat
}


 fun applyBoxBlur(srcMat: Mat, blurSize: Int): Mat {
    val dstMat = Mat()
    Imgproc.blur(srcMat, dstMat, Size(blurSize.toDouble(), blurSize.toDouble()))
    return dstMat
}
