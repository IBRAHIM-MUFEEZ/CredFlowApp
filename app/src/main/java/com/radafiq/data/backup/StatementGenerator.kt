package com.radafiq.data.backup

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.radafiq.data.models.CustomerSummary
import com.radafiq.data.models.CustomerTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class StatementGenerator(private val context: Context) {

    // ── Brand palette (matches RadafiqDarkColors) ─────────────────────────────
    private val BG_DEEP        = 0xFF0E0820.toInt()   // RadafiqNightDeep
    private val BG_SOFT        = 0xFF1A1030.toInt()   // RadafiqNightSoft
    private val BG_RAISED      = 0xFF231540.toInt()   // RadafiqNightRaised
    private val PRIMARY        = 0xFF667EEA.toInt()   // RadafiqPurple
    private val VIOLET         = 0xFF764BA2.toInt()   // RadafiqViolet
    private val PINK           = 0xFFF093FB.toInt()   // RadafiqPink
    private val RED_ACCENT     = 0xFFF5576C.toInt()   // RadafiqRed
    private val TEXT_PRIMARY   = 0xFFF0EEFF.toInt()   // RadafiqText
    private val TEXT_MUTED     = 0xFF9B8EC4.toInt()   // RadafiqMuted
    private val OUTLINE        = 0xFF3D2B6B.toInt()   // RadafiqOutline
    private val GREEN_SETTLED  = 0xFF4CAF50.toInt()
    private val ORANGE_PENDING = 0xFFFF9800.toInt()

    // ── Font loader — tries bundled Argentum Sans, falls back to system sans ──
    private fun loadTypeface(bold: Boolean = false): Typeface {
        val assetName = if (bold) "fonts/ArgentumSans-SemiBold.ttf" else "fonts/ArgentumSans-Regular.ttf"
        return runCatching {
            Typeface.createFromAsset(context.assets, assetName)
        }.getOrElse {
            Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    suspend fun generateStatement(
        customer: CustomerSummary,
        generatedByName: String = "Radafiq User"
    ): Result<Uri> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val regular = loadTypeface(bold = false)
                val bold    = loadTypeface(bold = true)

                val pdfDocument = PdfDocument()
                val pageWidth  = 595
                val pageHeight = 842
                var pageNumber = 1
                var yPosition  = 0

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                var page   = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas

                // Dark gradient background for every page
                drawPageBackground(canvas, pageWidth, pageHeight)

                yPosition = drawHeader(canvas, customer, pageWidth, 32, regular, bold)
                yPosition += 18

                yPosition = drawSummary(canvas, customer, pageWidth, yPosition, regular, bold)
                yPosition += 18

                yPosition = drawDuesSummary(canvas, customer, pageWidth, yPosition, regular, bold)
                yPosition += 18

                val transactions = customer.transactions
                    .filter { it.isVisibleInTransactions() }
                    .sortedByDescending { it.transactionDate }

                if (transactions.isNotEmpty()) {
                    yPosition = drawSectionHeader(canvas, "Transactions", pageWidth, yPosition, bold)
                    yPosition += 8

                    for (transaction in transactions) {
                        if (yPosition > pageHeight - 110) {
                            drawFooter(canvas, pageNumber, pageHeight, generatedByName, regular)
                            pdfDocument.finishPage(page)
                            pageNumber++
                            val newInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page   = pdfDocument.startPage(newInfo)
                            canvas = page.canvas
                            drawPageBackground(canvas, pageWidth, pageHeight)
                            yPosition = 40
                        }
                        yPosition = drawTransactionRow(canvas, transaction, pageWidth, yPosition, regular, bold)
                    }
                }

                // EMI schedule
                val emiTransactions = customer.transactions.filter { it.isEmi }
                if (emiTransactions.isNotEmpty()) {
                    if (yPosition > pageHeight - 160) {
                        drawFooter(canvas, pageNumber, pageHeight, generatedByName, regular)
                        pdfDocument.finishPage(page)
                        pageNumber++
                        val newInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page   = pdfDocument.startPage(newInfo)
                        canvas = page.canvas
                        drawPageBackground(canvas, pageWidth, pageHeight)
                        yPosition = 40
                    }
                    yPosition += 16
                    yPosition = drawEmiSchedule(canvas, emiTransactions, pageWidth, yPosition, regular, bold)
                }

                drawFooter(canvas, pageNumber, pageHeight, generatedByName, regular)
                pdfDocument.finishPage(page)

                val fileName = "statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
                val file = File(context.cacheDir, fileName)
                pdfDocument.writeTo(file.outputStream())
                pdfDocument.close()

                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }
    }

    // ── Page background: dark gradient matching app theme ─────────────────────
    private fun drawPageBackground(canvas: Canvas, pageWidth: Int, pageHeight: Int) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, pageHeight.toFloat(),
                intArrayOf(0xFF06030F.toInt(), BG_DEEP, BG_SOFT),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Subtle glow blobs (matching GlassBackdrop)
        val glow1 = Paint().apply { color = 0x2E667EEA.toInt() }
        val glow2 = Paint().apply { color = 0x1EF093FB.toInt() }
        val glow3 = Paint().apply { color = 0x18764BA2.toInt() }
        canvas.drawCircle(pageWidth * 0.18f, pageHeight * 0.10f, 90f, glow1)
        canvas.drawCircle(pageWidth * 0.92f, pageHeight * 0.18f, 75f, glow2)
        canvas.drawCircle(pageWidth * 0.76f, pageHeight * 0.86f, 70f, glow3)
    }

    // ── Header: logo + app name + title + customer name ───────────────────────
    private fun drawHeader(
        canvas: Canvas,
        customer: CustomerSummary,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        val logoSize = 44f
        val logoX    = 40f
        val logoY    = startY.toFloat()

        // Draw Radafiq logo (gradient circle + ر letterform)
        drawRadafiqLogo(canvas, logoX, logoY, logoSize)

        // App name beside logo
        val appNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = 18f
            typeface  = bold
            color     = PRIMARY
        }
        val appSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize  = 9f
            typeface  = regular
            color     = TEXT_MUTED
        }
        canvas.drawText("Radafiq", logoX + logoSize + 10f, logoY + 28f, appNamePaint)
        canvas.drawText("Customer Statement", logoX + logoSize + 10f, logoY + 42f, appSubPaint)

        // Divider line
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color       = OUTLINE
        }
        val lineY = logoY + logoSize + 12f
        canvas.drawLine(40f, lineY, (pageWidth - 40).toFloat(), lineY, linePaint)

        // Customer name + date
        val customerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            typeface = bold
            color    = TEXT_PRIMARY
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = regular
            color    = TEXT_MUTED
        }
        val generatedDate = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

        canvas.drawText(customer.name, 40f, lineY + 22f, customerPaint)
        canvas.drawText("Statement generated on $generatedDate", 40f, lineY + 36f, datePaint)

        return (lineY + 50f).toInt()
    }

    // ── Radafiq logo: gradient circle + ر letterform ─────────────────────────
    private fun drawRadafiqLogo(canvas: Canvas, x: Float, y: Float, size: Float) {
        val cx = x + size / 2f
        val cy = y + size / 2f
        val r  = size / 2f

        // Gradient circle background
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                x, y, x + size, y + size,
                intArrayOf(PRIMARY, VIOLET),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, r, circlePaint)

        // Subtle white border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = 1.2f
            color       = 0x33FFFFFF.toInt()
        }
        canvas.drawCircle(cx, cy, r - 0.8f, borderPaint)

        val strokeW = size * 0.09f

        // ر vertical stem
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = strokeW
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
            color       = 0xFFFFFFFF.toInt()
        }
        val stemPath = Path().apply {
            moveTo(cx - size * 0.04f, cy - size * 0.28f)
            lineTo(cx - size * 0.04f, cy + size * 0.18f)
            // bottom curve
            quadTo(
                cx - size * 0.04f, cy + size * 0.30f,
                cx + size * 0.04f, cy + size * 0.32f
            )
            quadTo(
                cx + size * 0.12f, cy + size * 0.30f,
                cx + size * 0.12f, cy + size * 0.18f
            )
            lineTo(cx + size * 0.12f, cy - size * 0.28f)
        }
        canvas.drawPath(stemPath, stemPaint)

        // ر left arc
        val arcPath = Path().apply {
            moveTo(cx - size * 0.04f, cy - size * 0.28f)
            quadTo(
                cx - size * 0.38f, cy - size * 0.32f,
                cx - size * 0.42f, cy + size * 0.02f
            )
            quadTo(
                cx - size * 0.42f, cy + size * 0.18f,
                cx - size * 0.16f, cy + size * 0.22f
            )
        }
        canvas.drawPath(arcPath, stemPaint)

        // Pink accent tail
        val tailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style       = Paint.Style.STROKE
            strokeWidth = size * 0.07f
            strokeCap   = Paint.Cap.ROUND
            shader      = LinearGradient(
                cx - size * 0.02f, cy + size * 0.22f,
                cx + size * 0.38f, cy + size * 0.38f,
                intArrayOf(PINK, RED_ACCENT),
                null,
                Shader.TileMode.CLAMP
            )
        }
        val tailPath = Path().apply {
            moveTo(cx - size * 0.02f, cy + size * 0.22f)
            quadTo(
                cx + size * 0.18f, cy + size * 0.30f,
                cx + size * 0.38f, cy + size * 0.38f
            )
        }
        canvas.drawPath(tailPath, tailPaint)

        // Accent dots
        val dot1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RED_ACCENT }
        val dot2 = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCF093FB.toInt() }
        canvas.drawCircle(cx - size * 0.44f, cy - size * 0.06f, size * 0.045f, dot1)
        canvas.drawCircle(cx + size * 0.38f, cy + size * 0.40f, size * 0.035f, dot2)
    }

    // ── Summary: 3 metric boxes ───────────────────────────────────────────────
    private fun drawSummary(
        canvas: Canvas,
        customer: CustomerSummary,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        val boxes = listOf(
            Triple("Total Used",      formatMoney(customer.totalAmount),    PRIMARY),
            Triple("Customer Paid",   formatMoney(customer.creditDueAmount), 0xFF4CAF50.toInt()),
            Triple("Balance Due",     formatMoney(customer.balance),         if (customer.balance > 0) RED_ACCENT else 0xFF4CAF50.toInt())
        )
        return drawMetricBoxRow(canvas, boxes, pageWidth, startY, regular, bold)
    }

    // ── Dues summary: paid vs unpaid transaction counts ───────────────────────
    private fun drawDuesSummary(
        canvas: Canvas,
        customer: CustomerSummary,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        val visible = customer.transactions.filter { it.isVisibleInTransactions() }
        val settled   = visible.count { it.isSettled }
        val partial   = visible.count { !it.isSettled && it.partialPaidAmount > 0 }
        val unpaid    = visible.count { !it.isSettled && it.partialPaidAmount <= 0 }
        val totalTxns = visible.size

        val amtSettled = visible.filter { it.isSettled }.sumOf { it.amount }
        val amtPartial = visible.filter { !it.isSettled && it.partialPaidAmount > 0 }.sumOf { it.amount - it.partialPaidAmount }
        val amtUnpaid  = visible.filter { !it.isSettled && it.partialPaidAmount <= 0 }.sumOf { it.amount }

        // Section header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = bold
            color    = TEXT_MUTED
        }
        canvas.drawText("DUES OVERVIEW", 40f, startY.toFloat(), headerPaint)

        val cardY    = startY + 8
        val cardH    = 62
        val gap      = 8
        val cardW    = (pageWidth - 80 - gap * 2) / 3

        data class DueBox(val label: String, val count: String, val amount: String, val color: Int)
        val dueBoxes = listOf(
            DueBox("Settled",       "$settled of $totalTxns", formatMoney(amtSettled), GREEN_SETTLED),
            DueBox("Partial Paid",  "$partial of $totalTxns", formatMoney(amtPartial), ORANGE_PENDING),
            DueBox("Unpaid",        "$unpaid of $totalTxns",  formatMoney(amtUnpaid),  RED_ACCENT)
        )

        dueBoxes.forEachIndexed { i, box ->
            val left  = 40f + i * (cardW + gap)
            val right = left + cardW
            val top   = cardY.toFloat()
            val bot   = (cardY + cardH).toFloat()
            val radius = 10f

            // Card fill
            val fillPaint = Paint().apply { this.color = BG_RAISED }
            canvas.drawRoundRect(RectF(left, top, right, bot), radius, radius, fillPaint)

            // Left accent bar — clip to card shape so it respects rounded corners
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = box.color }
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(RectF(left, top, right, bot), radius, radius, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawRect(RectF(left, top, left + 5f, bot), accentPaint)
            canvas.restore()

            // Border
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style       = Paint.Style.STROKE
                strokeWidth = 1f
                this.color  = OUTLINE
            }
            canvas.drawRoundRect(RectF(left, top, right, bot), radius, radius, borderPaint)

            // Label
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize   = 8f
                typeface   = regular
                this.color = TEXT_MUTED
            }
            canvas.drawText(box.label, left + 12f, top + 16f, labelPaint)

            // Count
            val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize   = 14f
                typeface   = bold
                this.color = box.color
            }
            canvas.drawText(box.count, left + 12f, top + 36f, countPaint)

            // Amount
            val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize   = 8f
                typeface   = regular
                this.color = TEXT_MUTED
            }
            canvas.drawText(box.amount, left + 12f, top + 52f, amtPaint)
        }

        return cardY + cardH + 8
    }

    // ── Generic metric box row ────────────────────────────────────────────────
    private fun drawMetricBoxRow(
        canvas: Canvas,
        boxes: List<Triple<String, String, Int>>,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        val gap    = 8
        val boxW   = (pageWidth - 80 - gap * (boxes.size - 1)) / boxes.size
        val boxH   = 52

        boxes.forEachIndexed { i, (label, value, color) ->
            val left  = 40f + i * (boxW + gap)
            val right = left + boxW
            val top   = startY.toFloat()
            val bot   = (startY + boxH).toFloat()

            val fillPaint = Paint().apply { this.color = BG_RAISED }
            canvas.drawRoundRect(RectF(left, top, right, bot), 10f, 10f, fillPaint)

            val borderPaint = Paint().apply {
                style       = Paint.Style.STROKE
                strokeWidth = 1f
                this.color  = OUTLINE
            }
            canvas.drawRoundRect(RectF(left, top, right, bot), 10f, 10f, borderPaint)

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize   = 8f
                typeface   = regular
                this.color = TEXT_MUTED
            }
            canvas.drawText(label, left + 10f, top + 16f, labelPaint)

            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize   = 13f
                typeface   = bold
                this.color = color
            }
            canvas.drawText(value, left + 10f, top + 38f, valuePaint)
        }

        return startY + boxH + 6
    }

    // ── Section header ────────────────────────────────────────────────────────
    private fun drawSectionHeader(
        canvas: Canvas,
        title: String,
        pageWidth: Int,
        startY: Int,
        bold: Typeface
    ): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = bold
            color    = TEXT_MUTED
        }
        canvas.drawText(title.uppercase(), 40f, startY.toFloat(), paint)

        val linePaint = Paint().apply {
            strokeWidth = 1f
            color       = OUTLINE
        }
        canvas.drawLine(40f, (startY + 5).toFloat(), (pageWidth - 40).toFloat(), (startY + 5).toFloat(), linePaint)

        return startY + 14
    }

    // ── Transaction row ───────────────────────────────────────────────────────
    private fun drawTransactionRow(
        canvas: Canvas,
        transaction: CustomerTransaction,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        val rowH   = 38
        val left   = 40f
        val right  = (pageWidth - 40).toFloat()

        // Row background
        val rowFill = Paint().apply { color = BG_RAISED }
        canvas.drawRoundRect(RectF(left, startY.toFloat(), right, (startY + rowH).toFloat()), 8f, 8f, rowFill)

        // Status color + left bar
        val statusColor = when {
            transaction.isSettled                          -> GREEN_SETTLED
            transaction.partialPaidAmount > 0              -> ORANGE_PENDING
            else                                           -> RED_ACCENT
        }
        val barPaint = Paint().apply { color = statusColor }
        canvas.drawRoundRect(RectF(left, startY.toFloat(), left + 4f, (startY + rowH).toFloat()), 4f, 4f, barPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            typeface = regular
            color    = TEXT_MUTED
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = bold
            color    = TEXT_PRIMARY
        }
        val accountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            typeface = regular
            color    = TEXT_MUTED
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = bold
            color    = PRIMARY
        }
        val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            typeface = regular
            color    = statusColor
        }

        val textTop = startY + 14f
        val textBot = startY + 28f

        canvas.drawText(transaction.transactionDate, left + 10f, textTop, datePaint)
        canvas.drawText(transaction.name, left + 80f, textTop, namePaint)
        canvas.drawText(transaction.accountName, left + 80f, textBot, accountPaint)

        val statusText = when {
            transaction.isSettled                     -> "✓ Settled"
            transaction.partialPaidAmount > 0         -> "~ Partial ${formatMoney(transaction.partialPaidAmount)}"
            else                                      -> "✗ Unpaid"
        }
        canvas.drawText(formatMoney(transaction.amount), right - 120f, textTop, amountPaint)
        canvas.drawText(statusText, right - 120f, textBot, statusPaint)

        // Separator
        val sepPaint = Paint().apply {
            strokeWidth = 0.5f
            color       = OUTLINE
        }
        canvas.drawLine(left, (startY + rowH).toFloat(), right, (startY + rowH).toFloat(), sepPaint)

        return startY + rowH + 4
    }

    // ── EMI schedule ──────────────────────────────────────────────────────────
    private fun drawEmiSchedule(
        canvas: Canvas,
        emiTransactions: List<CustomerTransaction>,
        pageWidth: Int,
        startY: Int,
        regular: Typeface,
        bold: Typeface
    ): Int {
        var yPos = drawSectionHeader(canvas, "EMI Schedule", pageWidth, startY, bold)
        yPos += 4

        val grouped = emiTransactions.groupBy { it.emiGroupId }

        for ((_, txns) in grouped) {
            val sorted = txns.sortedBy { it.emiIndex }
            val first  = sorted.first()

            val groupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10f
                typeface = bold
                color    = VIOLET
            }
            canvas.drawText("${first.name.substringBefore(" — EMI")} — ${sorted.size} instalments", 40f, yPos.toFloat(), groupPaint)
            yPos += 14

            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 9f
                typeface = regular
                color    = TEXT_MUTED
            }
            for (tx in sorted) {
                val statusColor = if (tx.isSettled) GREEN_SETTLED else ORANGE_PENDING
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor }
                canvas.drawCircle(50f, yPos - 3f, 3f, dot)
                canvas.drawText("EMI ${tx.emiIndex + 1}/${tx.emiTotal}", 60f, yPos.toFloat(), cellPaint)
                canvas.drawText(tx.transactionDate, 140f, yPos.toFloat(), cellPaint)
                canvas.drawText(formatMoney(tx.amount), (pageWidth - 100).toFloat(), yPos.toFloat(), cellPaint)
                yPos += 13
            }
            yPos += 8
        }

        return yPos
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        pageHeight: Int,
        generatedByName: String,
        regular: Typeface
    ) {
        // Footer divider
        val linePaint = Paint().apply {
            strokeWidth = 0.8f
            color       = OUTLINE
        }
        canvas.drawLine(40f, (pageHeight - 44).toFloat(), 555f, (pageHeight - 44).toFloat(), linePaint)

        val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            typeface = regular
            color    = PRIMARY
        }
        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8f
            typeface = regular
            color    = TEXT_MUTED
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))

        // Left: generated by actual user name
        canvas.drawText("Generated by: $generatedByName", 40f, (pageHeight - 28).toFloat(), leftPaint)

        // Center: timestamp
        canvas.drawText("Generated: $timestamp", 40f, (pageHeight - 16).toFloat(), rightPaint)

        // Right: page + branding
        canvas.drawText("Page $pageNumber  •  Radafiq", 430f, (pageHeight - 16).toFloat(), rightPaint)
    }

    private fun formatMoney(amount: Double): String = "₹${String.format("%.2f", amount)}"
}
