package com.samanramezani1377.woogit

import android.app.Application

class WooGitApplication:Application(){lateinit var composition:AppComposition;override fun onCreate(){super.onCreate();composition=AppComposition(this)}}
