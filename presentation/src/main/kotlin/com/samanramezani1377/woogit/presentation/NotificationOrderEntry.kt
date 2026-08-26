package com.samanramezani1377.woogit.presentation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import com.samanramezani1377.woogit.core.domain.entity.EntityId
import com.samanramezani1377.woogit.core.domain.entity.StoreId
import com.samanramezani1377.woogit.core.domain.error.CoreResult

@Composable fun NotificationOrderEntry(deps:WooGitPresentationDependencies,storeId:String,orderId:String){var state by remember{mutableStateOf<Any?>(null)};LaunchedEffect(storeId,orderId){state=when(val r=deps.getOrder(StoreId(storeId),EntityId(orderId))){is CoreResult.Success->r.value;is CoreResult.Failure->r.error}};Column(Modifier.fillMaxSize().padding(20.dp)){Text("سفارش #$orderId",style=MaterialTheme.typography.headlineSmall);when(val value=state){null->CircularProgressIndicator();is com.samanramezani1377.woogit.core.domain.model.Order->{Text("وضعیت: ${value.status}");Text("اقلام: ${value.items.size}")};else->Text("خطا: $value")}}}
