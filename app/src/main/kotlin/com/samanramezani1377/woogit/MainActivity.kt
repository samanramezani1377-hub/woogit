package com.samanramezani1377.woogit

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.samanramezani1377.woogit.presentation.NotificationOrderEntry
import com.samanramezani1377.woogit.presentation.V1WooGitApp

class MainActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val composition=(application as WooGitApplication).composition;val storeId=intent.getStringExtra("store_id");val orderId=intent.getLongExtra("order_id",-1L);setContent{MaterialTheme{if(storeId!=null&&orderId>0)NotificationOrderEntry(composition.presentation,storeId,orderId.toString())else V1WooGitApp(composition.v1Presentation)}}}}
