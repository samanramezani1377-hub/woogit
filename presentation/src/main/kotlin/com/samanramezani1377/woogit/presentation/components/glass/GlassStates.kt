package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable fun GlassPrimaryAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true)=GlassButton(label,onClick,modifier,enabled)
@Composable fun GlassDestructiveAction(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true)=GlassOutlinedButton(label,onClick,modifier,enabled)
@Composable fun GlassLoading(label:String="در حال بارگذاری…")=GlassCard{Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=GlassTokens.accent)};Text(label,Modifier.fillMaxWidth(),textAlign=TextAlign.Center,color=GlassTokens.muted)}
@Composable fun GlassSyncIndicator(label:String="در حال همگام‌سازی…")=GlassStatusBadge(label)
@Composable fun GlassEmptyState(message:String,actionLabel:String?=null,onAction:(()->Unit)?=null)=GlassCard{Text(message,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold);if(actionLabel!=null&&onAction!=null)GlassButton(actionLabel,onAction,Modifier.fillMaxWidth())}
@Composable fun GlassErrorState(message:String,retry:(()->Unit)?=null)=GlassCard{GlassStatusBadge("خطا");Text(message,color=androidx.compose.material3.MaterialTheme.colorScheme.error);if(retry!=null)GlassButton("تلاش دوباره",retry,Modifier.fillMaxWidth())}
@Composable fun GlassOfflineState(message:String="آفلاین هستید؛ داده‌های ذخیره‌شده نمایش داده می‌شوند.")=GlassCard{GlassStatusBadge("آفلاین");Text(message)}
@Composable fun GlassPendingState(count:Int=0)=GlassCard{GlassStatusBadge("در انتظار${if(count>0)" • $count"else""}");Text("تغییرات محلی منتظر همگام‌سازی هستند.")}
@Composable fun GlassConflictState(message:String="برای ادامه باید تعارض را بررسی کنید.")=GlassCard{GlassStatusBadge("تعارض");Text(message)}
