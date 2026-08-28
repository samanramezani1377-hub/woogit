package com.samanramezani1377.woogit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable fun GlassImageContainer(modifier:Modifier=Modifier,content:@Composable BoxScope.()->Unit){val shape=RoundedCornerShape(GlassTokens.radiusMd);Surface(modifier.fillMaxWidth(),shape=shape,color=Color.White.copy(alpha=.36f),border=BorderStroke(1f,Color.White.copy(alpha=.56f))){Box(Modifier.fillMaxWidth(),content=content)}}
