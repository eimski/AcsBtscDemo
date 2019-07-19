package com.dic.acsbtscdemo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import android.graphics.Point
import kotlin.experimental.or

/**
 * Implements functions to create a display image of smart-tag.
 * @author AIOI SYSTEMS CO., LTD.
 * @version 1.3.0
 */
class DisplayPainter {

    /**
     * Returns a created image.
     * @return Bitmap
     */
    var previewImage: Bitmap? = null
        private set
    private var mCanvas: Canvas? = null

    /**
     * Create a image data for smart-tag.
     * @return
     */
    val localDisplayImage: ByteArray
        get() = getImage(previewImage, false)

    constructor() {
        previewImage = Bitmap.createBitmap(200, 96, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(previewImage!!)
        clearDisplay()
    }

    constructor(displaySizeType: Int) {
        when (displaySizeType) {
            DISPLAY_TYPE_200X96 -> previewImage = Bitmap.createBitmap(200, 96, Bitmap.Config.ARGB_8888)
            DISPLAY_TYPE_264X176 -> previewImage = Bitmap.createBitmap(264, 176, Bitmap.Config.ARGB_8888)
            DISPLAY_TYPE_300X200 -> previewImage = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)
        }
        mCanvas = Canvas(previewImage!!)
        clearDisplay()
    }

    /**
     * Clears the image.
     */
    fun clearDisplay() {
        mCanvas!!.drawColor(Color.WHITE)
    }

    /**
     * Draws a text.
     * @param text
     * @param x
     * @param y
     */
    fun putText(
        text: String,
        x: Int,
        y: Int,
        size: Int
    ) {

        val paint = Paint()
        paint.isAntiAlias = false
        paint.color = Color.BLACK
        paint.textSize = size.toFloat()
        val metrics = paint.fontMetrics
        val top = y - metrics.top
        mCanvas!!.drawText(text, x.toFloat(), top, paint)
    }

    /**
     * Draws a line.
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @param size
     * @param invert 反転する場合はtrue
     * @param dashed 点線を引く場合はtrue、実線はfalse。
     */
    @JvmOverloads
    fun putLine(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        size: Int,
        invert: Boolean = false,
        dashed: Boolean = false
    ) {

        val paint = Paint()
        paint.isAntiAlias = false
        if (invert) {
            paint.color = Color.WHITE
        } else {
            paint.color = Color.BLACK
        }
        paint.strokeWidth = size.toFloat()
        paint.style = Paint.Style.STROKE
        if (dashed) {
            paint.pathEffect = DashPathEffect(floatArrayOf(2.0f, 2.0f), 0.0f)
        }
        mCanvas!!.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
    }

    /**
     * Draws a rectangle.
     * @param x
     * @param y
     * @param width
     * @param height
     * @param border
     */
    fun putRectangle(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        border: Int
    ) {

        val paint = Paint()
        paint.isAntiAlias = false
        paint.color = Color.BLACK
        paint.strokeWidth = border.toFloat()
        paint.style = Paint.Style.STROKE

        mCanvas!!.drawRect(x.toFloat(), y.toFloat(), (x + width - 1).toFloat(), (y + height - 1).toFloat(), paint)
    }

    /**
     * Draws a bitmap.
     * @param bitmap
     * @param x
     * @param y
     * @param dither
     */
    fun putImage(bitmap: Bitmap?, x: Int, y: Int, dither: Boolean) {
        var bitmap: Bitmap? = bitmap ?: return

        if (dither) {
            bitmap = getDitheredImage(bitmap!!)
        } else {
            bitmap = getThresholdImage(bitmap!!)
        }

        val canvas = Canvas(previewImage!!)
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }

    companion object {

        val DISPLAY_TYPE_200X96 = 1
        val DISPLAY_TYPE_264X176 = 2
        val DISPLAY_TYPE_300X200 = 4

        /**
         * Gets a block and white image with dithering.
         * @return Bitmap
         */
        private fun getDitheredImage(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height

            val src = getGrayPixels(bitmap)
            val dst = CharArray(width * height)

            //Floyd & Steinberg
            val df = arrayOf(doubleArrayOf(-1.0, -1.0, 7.0 / 16.0), doubleArrayOf(3.0 / 16.0, 5.0 / 16.0, 1.0 / 16.0))
            val dfRows = df.size
            val dfCols = df[0].size
            val xRange = (dfCols - 1) / 2

            var d: Boolean
            var err: Double
            var xx: Int
            var yy: Int

            var index = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    index = x + y * width
                    val pixel = src[index]

                    if (pixel.toInt() > 127) {
                        d = true
                        dst[index] = 255.toChar()
                    } else {
                        d = false
                    }

                    if (d) {
                        err = (pixel.toInt() - 255).toDouble()
                    } else {
                        err = pixel.toDouble()
                    }

                    for (iy in 0 until dfRows) {
                        for (ix in -xRange..xRange) {
                            xx = x + ix

                            if ((xx < 0) or (xx > width - 1)) {
                                continue
                            }
                            yy = y + iy
                            if (yy > height - 1) {
                                continue
                            }
                            if (df[iy][ix + xRange] < 0) {
                                continue
                            }

                            val wk = src[xx + yy * width].toDouble() + err * df[iy][ix + xRange]
                            src[xx + yy * width] = adjustByte(wk)
                        }
                    }
                }
            }

            return createBlackWhiteImage(dst, width, height)
        }

        private fun adjustByte(value: Double): Char {
            var value = value
            if (value < 0) {
                value = 0.0
            } else if (value > 255) {
                value = 255.0
            }
            return value.toChar()
        }

        /**
         * カラー画像からグレイスケールのピクセル配列データを取得する
         * Gets a grayscaled image from a colored image.
         * @param bitmap
         * @return
         */
        private fun getGrayPixels(bitmap: Bitmap): CharArray {
            val width = bitmap.width
            val height = bitmap.height

            val src = IntArray(width * height)
            bitmap.getPixels(src, 0, width, 0, 0, width, height)
            val range = getPixelRange(src)
            val adjust = 255.toFloat() / (range.y - range.x)

            val dst = CharArray(width * height)

            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    index = x + y * width
                    val pixel = src[index]

                    var tmp = getAverage(
                        Color.red(pixel),
                        Color.green(pixel),
                        Color.blue(pixel)
                    ).toChar()

                    tmp -= range.x
                    dst[index] = (tmp.toFloat() * adjust).toChar()
                }
            }
            return dst
        }

        /**
         * Creates a block and white image.
         * @param pixels
         * @return Bitmap
         */
        private fun createBlackWhiteImage(pixels: CharArray, width: Int, height: Int): Bitmap {
            val pixelsInt = IntArray(width * height)

            var index: Int
            var value: Int
            for (y in 0 until height) {
                for (x in 0 until width) {
                    index = x + y * width
                    value = pixels[index].toInt()
                    pixelsInt[index] = Color.argb(255, value, value, value)
                }
            }
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixelsInt, 0, width, 0, 0, width, height)
            return bmp
        }

        /**
         * Convert to a black and white image with threshold.
         * @param bitmap
         * @return Bitmap
         */
        private fun getThresholdImage(bitmap: Bitmap): Bitmap {
            val width = bitmap.width
            val height = bitmap.height

            val src = getGrayPixels(bitmap)
            val dst = CharArray(width * height)

            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    index = x + y * width
                    val pixel = src[index]
                    if (pixel.toInt() > 127) {
                        dst[index] = 255.toChar()
                    } else {
                        dst[index] = 0.toChar()
                    }
                }
            }
            return createBlackWhiteImage(dst, width, height)
        }

        /**
         * Create a image data for smart-tag.
         * @param bitmap
         * @param dither
         * @return byte[]
         * @since 1.1.0
         */
        fun getImage(bitmap: Bitmap?, dither: Boolean): ByteArray {
            var bitmap = bitmap

            if (dither) {
                bitmap = getDitheredImage(bitmap!!)
            } else {
                bitmap = getThresholdImage(bitmap!!)
            }

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val cols = (width + 7) / 8
            val size = cols * height
            val result = ByteArray(size)

            var index = 0
            var bitPos = 7
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[x + y * width]

                    if (Color.red(pixel) == 0) {
                        val color = 1
                        result[index] = result[index] or (color shl bitPos).toByte()
                    }

                    bitPos--
                    if (bitPos < 0 || x == width - 1/*1ラインの最終ピクセル*/) {
                        index++
                        bitPos = 7
                    }
                }
            }
            return result
        }

        private fun getAverage(red: Int, green: Int, blue: Int): Int {
            //return (int)((float)(red + green + blue) / 3);
            //NTSC加重平均法
            return (red * 0.298912f
                    + green * 0.586611f
                    + blue * 0.114478f).toInt()
        }

        private fun getPixelRange(pixels: IntArray): Point {

            var max = 0
            var min = 255
            for (i in pixels.indices) {
                val pixel = pixels[i]

                val value = getAverage(
                    Color.red(pixel),
                    Color.green(pixel),
                    Color.blue(pixel)
                )
                max = Math.max(value, max)
                min = Math.min(value, min)
            }
            return Point(min, max)
        }
    }
}
