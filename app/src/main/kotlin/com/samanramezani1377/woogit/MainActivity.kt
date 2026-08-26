package com.samanramezani1377.woogit

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val composition=(application as WooGitApplication).composition;setContent{MaterialTheme{com.samanramezani1377.woogit.presentation.WooGitApp(composition.presentation)}}}}
