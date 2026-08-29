package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.samanramezani1377.woogit.core.domain.model.Product
import com.samanramezani1377.woogit.core.domain.model.ProductStatus
import com.samanramezani1377.woogit.core.domain.model.ProductType
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTokens
import com.samanramezani1377.woogit.presentation.GlassTopBar
import com.samanramezani1377.woogit.presentation.toPersianPrice

private fun ProductStatus.toPersianLabel(): String = when (this) {
    ProductStatus.PUBLISHED -> "منتشر شده"
    ProductStatus.DRAFT -> "پیش‌نویس"
    ProductStatus.PENDING -> "در انتظار"
    ProductStatus.PRIVATE -> "خصوصی"
    ProductStatus.OTHER -> "سایر"
}

private fun ProductType.toPersianLabel(): String = when (this) {
    ProductType.SIMPLE -> "ساده"
    ProductType.VARIABLE -> "متغیر"
    ProductType.GROUPED -> "گروهی"
    ProductType.EXTERNAL -> "خارجی"
    ProductType.OTHER -> "سایر"
}

private fun String?.toPersianPriceOrDash(): String = this?.trim()?.takeIf { it.isNotBlank() }?.toPersianPrice()?.let { "$it تومان" } ?: "—"

@Composable
internal fun ProductDetailScreen(product: Product,onBack:()->Unit,onEdit:()->Unit,onVariations:()->Unit={}){
    GlassScaffold{padding->LazyColumn(Modifier.fillMaxSize().padding(padding),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(16.dp,12.dp,16.dp,104.dp)){
        item{GlassTopBar(product.name,"جزئیات و مدیریت محصول")}
        item{GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            GlassText("وضعیت: ${product.status.toPersianLabel()}")
            GlassText("نوع: ${product.type.toPersianLabel()}")
            GlassText("SKU: ${product.sku ?: "—"}")
            GlassText("قیمت اصلی: ${product.pricing.regular.toPersianPriceOrDash()}")
            GlassText("قیمت فروش: ${product.pricing.sale.toPersianPriceOrDash()}")
            GlassText("موجودی: ${product.stock?.quantity?.toString()?.removeSuffix(".0") ?: "—"}",style=MaterialTheme.typography.bodyMedium.copy(color=GlassTokens.muted))
        }}}
        item{GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassText("تصاویر");if(product.images.isEmpty())GlassText("تصویری ثبت نشده است.")else product.images.forEach{img->AsyncImage(model=img.src,contentDescription=img.alt?:img.name,modifier=Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)}}}}
        item{GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassText("دسته‌بندی‌ها");if(product.categories.isEmpty())GlassText("دسته‌بندی‌ای ثبت نشده است.")else product.categories.forEach{GlassText(it.name)}}}}
        item{GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassText("ویژگی‌ها");if(product.attributes.isEmpty())GlassText("ویژگی‌ای ثبت نشده است.")else product.attributes.forEach{GlassText("${it.name}: ${it.options.joinToString("، ")}")}}}}
        item{GlassPrimaryAction("مدیریت تنوع‌ها",onVariations)}
        item{GlassPrimaryAction("ویرایش محصول",onEdit)}
        item{GlassPrimaryAction("بازگشت",onBack)}
    }}
}
