package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun GlassDialog(show:Boolean,title:String,onDismiss:()->Unit,confirmLabel:String="تأیید",onConfirm:()->Unit,content:@Composable ColumnScope.()->Unit){if(show)AlertDialog(onDismissRequest=onDismiss,title={Text(title,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)},text={Column(content=content)},confirmButton={GlassButton(confirmLabel,onConfirm)},dismissButton={GlassTextButton("انصراف",onDismiss)},containerColor=Color(0xFFF7F8FC).copy(alpha=.96f),shape=RoundedCornerShape(26.dp))}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun GlassBottomSheet(show:Boolean,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
    val background = AppBackgroundThemeStore.selected.color
    val surface = if (AppBackgroundThemeStore.selected == AppBackgroundTheme.DARK) {
        Color.White.copy(alpha = .14f)
    } else {
        background.copy(alpha = .94f)
    }
    if(show)ModalBottomSheet(onDismissRequest=onDismiss,containerColor=surface,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)){Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),content=content)}}
@Composable fun GlassSnackbar(hostState:SnackbarHostState,modifier:Modifier=Modifier)=SnackbarHost(hostState,modifier)
