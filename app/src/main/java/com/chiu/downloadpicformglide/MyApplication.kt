package com.chiu.downloadpicformglide

import android.app.Application
import kotlin.properties.Delegates

/**
 *
 *
 * @author : zfz
 * e-mail : 137776369@163.com
 * time   : 2020/09/22
 * desc   :
 */
class MyApplication : Application() {

    companion object {
        var context: MyApplication by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }

}