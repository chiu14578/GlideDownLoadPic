package com.chiu.downloadpicformglide

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTv.setOnClickListener {
            RxPermissions(this).request(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).subscribe {
                downloadPicByGlide(
                    this,
                    "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1280325423,1024589167&fm=26&gp=0.jpg"
                )
            }
        }
    }


    private fun downloadPicByGlide(context: Context, picUrl: String) {
        OperatePicUtil.instance.savePicByBm(context, picUrl)
    }


}