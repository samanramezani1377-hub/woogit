package com.samanramezani1377.woogit.presentation

import android.text.Html
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object GlassTokens { val radiusSm=12.dp; val radiusMd=18.dp; val radiusLg=26.dp; val spacingXs=4.dp; val spacingSm=8.dp; val spacingMd=12.dp; val spacingLg=18.dp; val spacingXl=26.dp; val glassFill=Color.White.copy(alpha=.52f); val glassFillStrong=Color.White.copy(alpha=.72f); val glassBorder=Color.White.copy(alpha=.65f); val glassHighlight=Color.White.copy(alpha=.78f); val ink=Color(0xFF1B1F2A); val muted=Color(0xFF4B5263); val faint=Color(0xFF767D8C); val accent=Color(0xFF6C5CE7); val accentSecondary=Color(0xFFE84393); val urgent=Color(0xFFFF6B4A); val live=Color(0xFF22C55E); val badge=Color(0xFFEF4444) }

internal fun String.glassLabel():String=when(lowercase()){"published"->"منتشر شده";"draft"->"پیش‌نویس";"pending"->"در انتظار";"private"->"خصوصی";"other"->"سایر";"in_stock","instock"->"موجود";"out_of_stock","outofstock"->"ناموجود";"on_backorder","onbackorder"->"پیش‌سفارش";"simple"->"ساده";"grouped"->"گروهی";"external"->"خارجی";"variable"->"متغیر";"processing"->"در حال پردازش";"on_hold"->"در انتظار";"completed"->"تکمیل شده";"cancelled"->"لغو شده";"refunded"->"مسترد شده";"failed"->"ناموفق";"connected"->"متصل";"offline"->"آفلاین";"conflict"->"تعارض";"syncing"->"در حال همگام‌سازی";"succeeded"->"موفق";else->this}
internal fun String.stripHtml():String=Html.fromHtml(this,Html.FROM_HTML_MODE_LEGACY).toString()
