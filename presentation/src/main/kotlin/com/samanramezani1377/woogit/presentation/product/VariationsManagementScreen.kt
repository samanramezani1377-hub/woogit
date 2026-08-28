package com.samanramezani1377.woogit.presentation.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samanramezani1377.woogit.presentation.GlassCard
import com.samanramezani1377.woogit.presentation.GlassEmptyState
import com.samanramezani1377.woogit.presentation.GlassPrimaryAction
import com.samanramezani1377.woogit.presentation.GlassScaffold
import com.samanramezani1377.woogit.presentation.GlassStatusBadge
import com.samanramezani1377.woogit.presentation.GlassText
import com.samanramezani1377.woogit.presentation.GlassTopBar

@Composable
internal fun VariationsManagementScreen(variations:List<VariationUiModel>,onAdd:()->Unit,onEdit:(String)->Unit,modifier:Modifier=Modifier){
    GlassScaffold(modifier){padding->Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
        GlassTopBar("تنوع‌های محصول","مدیریت قیمت، موجودی و ویژگی‌ها")
        GlassPrimaryAction("افزودن تنوع",onAdd)
        if(variations.isEmpty())GlassEmptyState("برای این محصول تنوعی ثبت نشده است.")
        variations.forEach{v->GlassCard{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){GlassStatusBadge("تنوع");GlassText(v.title);GlassText("قیمت: ${v.price}");GlassText("موجودی: ${v.stock}");GlassPrimaryAction("مشاهده و ویرایش",{onEdit(v.id)})}}}
    }}
}
