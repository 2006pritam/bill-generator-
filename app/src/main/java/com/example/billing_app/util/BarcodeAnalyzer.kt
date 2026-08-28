package com.example.billing_app.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader()
    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < 250) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val planes = imageProxy.planes
                val yBuffer: ByteBuffer = planes[0].buffer
                val ySize = yBuffer.remaining()
                val data = ByteArray(ySize)
                yBuffer.get(data)

                val width = imageProxy.width
                val height = imageProxy.height

                val source = PlanarYUVLuminanceSource(
                    data,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(binaryBitmap)

                result?.text?.let { rawBarcode ->
                    if (rawBarcode.isNotBlank()) {
                        lastAnalyzedTimestamp = currentTimestamp
                        onBarcodeDetected(rawBarcode.trim())
                    }
                }
            } catch (_: Exception) {
                // Ignore frame decode exceptions
            } finally {
                reader.reset()
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}
