package com.samanramezani1377.woogit.commerce

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.samanramezani1377.woogit.core.domain.model.InvoiceDocument
import java.io.ByteArrayOutputStream

class InvoicePdfRenderer {
    fun render(document: InvoiceDocument): ByteArray {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = android.graphics.Color.BLACK
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = android.graphics.Color.BLACK
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            color = android.graphics.Color.BLACK
        }

        var pageNumber = 1
        var page = pdf.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
        )
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
            )
            canvas = page.canvas
            y = margin
        }

        fun line(text: String, textPaint: Paint = paint, spacing: Float = 18f) {
            if (y > pageHeight - margin - spacing) {
                newPage()
            }
            canvas.drawText(text, margin, y, textPaint)
            y += spacing
        }

        line(document.storeName, titlePaint, 28f)
        line("Invoice #${document.orderNumber}", boldPaint)
        line("Currency: ${document.currency}")
        line("Customer: ${document.customerName}")
        document.customerEmail?.takeIf { it.isNotBlank() }?.let {
            line("Email: $it")
        }
        document.customerPhone?.takeIf { it.isNotBlank() }?.let {
            line("Phone: $it")
        }
        document.paymentMethod?.takeIf { it.isNotBlank() }?.let {
            line("Payment: $it")
        }

        y += 10f
        line("Items", boldPaint)
        document.lines.forEach { item ->
            line("${item.name}  x${item.quantity}  ${item.total}")
        }

        y += 10f
        line("Shipping: ${document.shippingTotal}")
        line("Discount: ${document.discountTotal}")
        line("Total: ${document.grandTotal}", boldPaint)

        pdf.finishPage(page)

        return ByteArrayOutputStream().use { output ->
            pdf.writeTo(output)
            pdf.close()
            output.toByteArray()
        }
    }
}
