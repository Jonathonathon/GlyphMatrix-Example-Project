package com.nothinglondon.sdkdemo.demos.cardspinner

import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils
import kotlinx.coroutines.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CardSpinnerService : GlyphMatrixService("Card-Spinner") {
    private val suits = listOf("♠", "♥", "♦", "♣")
    private val ranks = listOf("A","2","3","4","5","6","7","8","9","10","J","Q","K")

    // Mini font for drawing rank/suit
    private val miniFont = mapOf(
        "A" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(1,1,1),
            byteArrayOf(1,0,1),
            byteArrayOf(1,0,1)
        ),
        "2" to arrayOf(
            byteArrayOf(1,1,0),
            byteArrayOf(0,0,1),
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,0),
            byteArrayOf(1,1,1)
        ),
        "3" to arrayOf(
            byteArrayOf(1,1,0),
            byteArrayOf(0,0,1),
            byteArrayOf(0,1,0),
            byteArrayOf(0,0,1),
            byteArrayOf(1,1,0)
        ),
        "4" to arrayOf(
            byteArrayOf(1,0,1),
            byteArrayOf(1,0,1),
            byteArrayOf(1,1,1),
            byteArrayOf(0,0,1),
            byteArrayOf(0,0,1)
        ),
        "5" to arrayOf(
            byteArrayOf(1,1,1),
            byteArrayOf(1,0,0),
            byteArrayOf(1,1,0),
            byteArrayOf(0,0,1),
            byteArrayOf(1,1,0)
        ),
        "6" to arrayOf(
            byteArrayOf(0,1,1),
            byteArrayOf(1,0,0),
            byteArrayOf(1,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,0)
        ),
        "7" to arrayOf(
            byteArrayOf(1,1,1),
            byteArrayOf(0,0,1),
            byteArrayOf(0,1,0),
            byteArrayOf(0,1,0),
            byteArrayOf(0,1,0)
        ),
        "8" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,0)
        ),
        "9" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,1),
            byteArrayOf(0,0,1),
            byteArrayOf(1,1,0)
        ),
        "10" to arrayOf(
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0),
            byteArrayOf(0,1,0),
            byteArrayOf(0,1,0),
            byteArrayOf(0,1,0)
            // We're just going to use a T for Ten
        ),
        "J" to arrayOf(
            byteArrayOf(0,1,1),
            byteArrayOf(0,0,1),
            byteArrayOf(0,0,1),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,0)
        ),
        "Q" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,0),
            byteArrayOf(0,0,1)
        ),
        "K" to arrayOf(
            byteArrayOf(1,0,1),
            byteArrayOf(1,0,1),
            byteArrayOf(1,1,0),
            byteArrayOf(1,0,1),
            byteArrayOf(1,0,1)
        ),
        "♠" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0)
        ),
        "♥" to arrayOf(
            byteArrayOf(1,0,1),
            byteArrayOf(1,1,1),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0),
            byteArrayOf(0,0,0)
        ),
        "♦" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,1),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0),
            byteArrayOf(0,0,0)
        ),
        "♣" to arrayOf(
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0),
            byteArrayOf(1,1,1),
            byteArrayOf(0,1,0)
        )
    )

    private var gm: GlyphMatrixManager? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var currentRank: String? = null
    private var currentSuit: String? = null
    private var showingFace = false

    private fun drawChar(frame: Array<ByteArray>, char: String, centerX: Int, centerY: Int) {
        // <- val bitmap is declared here inside the function
        val bitmap: Array<ByteArray> = miniFont[char] ?: return
        val h = bitmap.size
        val w = bitmap[0].size
        val startX = centerX - w / 2
        val startY = centerY - h / 2
        for (y in 0 until h) {
            for (x in 0 until w) {
                val px = startX + x
                val py = startY + y
                if (px in 0..24 && py in 0..24) {
                    frame[px][py] = bitmap[y][x]
                }
            }
        }
    }

    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        gm = glyphMatrixManager
        drawCardBack()
    }

    // Initial Code Block
    private fun frameToBitmap(frame: Array<ByteArray>): Bitmap {
        val size = frame.size
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val pixelOn = frame[x][y] == 1.toByte()
                bmp.setPixel(x, y, if (pixelOn) Color.WHITE else Color.BLACK)
            }
        }
        return bmp
    }

    fun onAction() {
        spinAndRevealCard()
    }

    fun onFlipGesture() {
        spinAndRevealCard()
    }

    private fun spinAndRevealCard() {
        scope.launch {
            // Step 1: Spin animation (3 flips)
            repeat(3) { _ ->
                if (showingFace) drawCardFace(currentRank!!, currentSuit!!) else drawCardBack()
                delay(80)
                drawCardBackHalf()   // thin flip effect
                delay(80)
                clearMatrix()
                delay(80)
            }

            if (!showingFace) {
                // Reveal a new random card
                currentSuit = suits.random()
                currentRank = ranks.random()
                drawCardFace(currentRank!!, currentSuit!!)
                showingFace = true
            } else {
                // Flip back to card back
                drawCardBack()
                showingFace = false
            }
        }
    }

    // Draw a “thin” card outline to simulate flipping
    private fun drawCardBackHalf() {
        val frame = getEmptyFrame()
        for (x in 9..15) {       // thinner horizontal
            frame[x][4] = 1
            frame[x][20] = 1
        }
        for (y in 4..20) {
            frame[9][y] = 1      // thinner vertical
            frame[15][y] = 1
        }

        // Simple checkerboard pattern inside the thin outline
        for (x in 10..14) {
            for (y in 5..19) {
                if ((x + y) % 2 == 0) frame[x][y] = 1
            }
        }

        val bmp = frameToBitmap(frame)
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bmp)
            .build()

        val matrixFrame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        gm?.setMatrixFrame(matrixFrame.render())
    }

    private fun getEmptyFrame(): Array<ByteArray> {
        val size = 25 // Glyph Matrix is 25x25
        return Array(size) { ByteArray(size) { 0 } }
    }
    private fun drawCardBack() {
        val frame = getEmptyFrame()
        // Border
        for (x in 4..20) {
            frame[x][4] = 1
            frame[x][20] = 1
        }
        for (y in 4..20) {
            frame[4][y] = 1
            frame[20][y] = 1
        }

        // Checkerboard pattern
        for (x in 5..19) {
            for (y in 5..19) {
                if ((x + y) % 2 == 0) {
                    frame[x][y] = 1
                }
            }
        }

        val bmp = frameToBitmap(frame)
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bmp)
            .build()

        val matrixFrame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        gm?.setMatrixFrame(matrixFrame.render())
    }

    private fun drawCardFace(rank: String, suit: String) {
        val frame = getEmptyFrame()
        // Card outline
        for (x in 4..20) {
            frame[x][4] = 1
            frame[x][20] = 1
        }
        for (y in 4..20) {
            frame[4][y] = 1
            frame[20][y] = 1
        }

        // Draw rank above suit
        drawChar(frame, rank, centerX = 12, centerY = 10)
        drawChar(frame, suit, centerX = 12, centerY = 14)

        // Convert to bitmap and render
        val bmp = frameToBitmap(frame)
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bmp)
            .build()

        val matrixFrame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        gm?.setMatrixFrame(matrixFrame.render())
    }

    private fun clearMatrix() {
        val frame = getEmptyFrame()

        val bmp = frameToBitmap(frame)
        val matrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bmp)
            .build()

        val matrixFrame = GlyphMatrixFrame.Builder()
            .addTop(matrixObject)
            .build(applicationContext)

        gm?.setMatrixFrame(matrixFrame.render())
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}