package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable fun GlassStatusBadge(label:String,modifier:Modifier=Modifier){Surface(modifier,shape=RoundedCornerShape(50),color=Color.White.copy(alpha=.36f),border=BorderStroke(1.dp,Color.White.copy(alpha=.62f))){Row(Modifier.padding(horizontal=11.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){val live=label.equals("Connected",true)||label.equals("connected",true)||label=="متصل";Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if(live)GlassTokens.live else GlassTokens.accent));Text(label.glassLabel(),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.SemiBold)}}}

@Composable fun GlassListItem(title:String,subtitle:String?=null,modifier:Modifier=Modifier,onClick:(()->Unit)?=null,trailing:(@Composable()->Unit)?=null){val shape=RoundedCornerShape(18.dp);Surface(modifier.fillMaxWidth().heightIn(min=72.dp),shape=shape,color=Color.White.copy(alpha=.38f),border=BorderStroke(1.dp,Color.White.copy(alpha=.58f))){Row(Modifier.fillMaxWidth().then(if(onClick!=null)Modifier.clickable(role=Role.Button,onClick=onClick)else Modifier).padding(horizontal=16.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFFC6E6FF).copy(alpha=.72f),Color(0xFFD8CEFF).copy(alpha=.72f)))),contentAlignment=Alignment.Center){Text("•",color=GlassTokens.accent,fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(title.stripHtml(),style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold,color=GlassTokens.ink);if(!subtitle.isNullOrBlank())Text(subtitle.stripHtml(),style=MaterialTheme.typography.bodySmall,color=GlassTokens.muted)};trailing?.invoke()}}}
