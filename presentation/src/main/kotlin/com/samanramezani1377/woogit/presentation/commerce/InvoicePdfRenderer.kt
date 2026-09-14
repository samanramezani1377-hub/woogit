package com.samanramezani1377.woogit.presentation.commerce

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.samanramezani1377.woogit.core.domain.model.InvoiceDocument
import java.io.ByteArrayOutputStream

internal class InvoicePdfRenderer {
    fun render(document: InvoiceDocument): ByteArray {
        val pdf = PdfDocument()
        val width = 595
        val height = 842
        val margin = 40f
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = Typeface.DEFAULT_BOLD }
        val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            pdf.finishPage(page)
            pageNumber += 1
            page = pdf.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page.canvas
            y = margin
        }
        fun line(text: String, paint: Paint = normal, spacing: Float = 18f) {
            if (y > height - margin - spacing) newPage()
            canvas.drawText(text, margin, y, paint)
            y += spacing
        }

        line(document.storeName, title, 28f)
        line("Invoice #${document.orderNumber}", bold)
        line("Currency: ${document.currency}")
        line("Customer: ${document.customerName}")
        document.customerEmail?.takeIf(String::isNotBlank)?.let { line("Email: $it") }
        document.customerPhone?.takeIf(String::isNotBlank)?.let { line("Phone: $it") }
        document.paymentMethod?.takeIf(String::isNotBlank)?.let { line("Payment: $it") }
        y += 10f
        line("Items", bold)
        document.lines.forEach { line("${it.name}  x${it.quantity}  ${it.total}") }
        y += 10f
        line("Shipping: ${document.shippingTotal}")
        line("Discount: ${document.discountTotal}")
        line("Total: ${document.grandTotal}", bold)
        pdf.finishPage(page)
        return ByteArrayOutputStream().use { output -> pdf.writeTo(output); pdf.close(); output.toByteArray() }
    }
}
