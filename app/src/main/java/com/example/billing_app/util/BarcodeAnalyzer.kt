package com.example.billing_app.util

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.DATA_MATRIX
            ),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    private var lastAnalyzedTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < 200) {
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
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                // Rotate luminance buffer so 1D and 2D barcodes scan correctly in portrait and landscape
                val (processedData, finalWidth, finalHeight) = when (rotationDegrees) {
                    90 -> Triple(rotateYUV90(data, width, height), height, width)
                    180 -> Triple(rotateYUV180(data, width, height), width, height)
                    270 -> Triple(rotateYUV270(data, width, height), height, width)
                    else -> Triple(data, width, height)
                }

                val source = PlanarYUVLuminanceSource(
                    processedData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                var result = try {
                    reader.decodeWithState(binaryBitmap)
                } catch (_: Exception) {
                    null
                }

                // If decoding fails, try with rotated orientations (0°, 90°, 270°) and inverted luminance
                if (result == null) {
                    try {
                        val fallbackSource = PlanarYUVLuminanceSource(
                            data,
                            width,
                            height,
                            0,
                            0,
                            width,
                            height,
                            false
                        )
                        result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(fallbackSource)))
                    } catch (_: Exception) {
                        // Fallback failed
                    }
                }

                if (result == null) {
                    try {
                        val rot90 = rotateYUV90(data, width, height)
                        val rot90Source = PlanarYUVLuminanceSource(
                            rot90,
                            height,
                            width,
                            0,
                            0,
                            height,
                            width,
                            false
                        )
                        result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(rot90Source)))
                    } catch (_: Exception) {
                        // 90 deg rotation fallback failed
                    }
                }

                if (result == null) {
                    try {
                        val rot270 = rotateYUV270(data, width, height)
                        val rot270Source = PlanarYUVLuminanceSource(
                            rot270,
                            height,
                            width,
                            0,
                            0,
                            height,
                            width,
                            false
                        )
                        result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(rot270Source)))
                    } catch (_: Exception) {
                        // 270 deg rotation fallback failed
                    }
                }

                result?.text?.let { rawBarcode ->
                    val cleanCode = rawBarcode.trim()
                    if (cleanCode.isNotBlank()) {
                        lastAnalyzedTimestamp = currentTimestamp
                        onBarcodeDetected(cleanCode)
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

    private fun rotateYUV90(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        var k = 0
        for (x in 0 until width) {
            for (y in height - 1 downTo 0) {
                rotated[k++] = data[y * width + x]
            }
        }
        return rotated
    }

    private fun rotateYUV180(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        for (i in 0 until data.size) {
            rotated[i] = data[data.size - 1 - i]
        }
        return rotated
    }

    private fun rotateYUV270(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        var k = 0
        for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                rotated[k++] = data[y * width + x]
            }
        }
        return rotated
    }
}

