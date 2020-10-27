package com.chiu.downloadpicformglide

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*

/**
 *
 *
 * @author : chiu
 * e-mail : 137776369@163.com
 * time   : 2020/09/21
 * desc   :
 */
class OperatePicUtil {

    companion object {
        val instance: OperatePicUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            OperatePicUtil()
        }
    }

    /**
     * 通过bitmap
     */
    @SuppressLint("CheckResult")
    fun savePicByBm(context: Context, picUrl: String) {
        Glide.with(context).asBitmap().load(picUrl).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Observable.create(ObservableOnSubscribe<Boolean> {
                    val destFile = createSaveFile(
                        context,
                        false,
                        "${System.currentTimeMillis()}.jpg",
                        "your_picture_save_path"
                    )
                    saveBitmap2SelfDirectroy(
                        context,
                        resource,
                        destFile
                    )

                    it.onNext(true)
                    it.onComplete()
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (it) {
                            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        })
    }

    /**
     * 通过downloadOnly<File>保存
     */
    @SuppressLint("CheckResult")
    fun savePicByFile(context: Context, picUrl: String) {
        Observable.create(ObservableOnSubscribe<File?> { emitter ->
            Glide.with(context)
                .downloadOnly()
                .load(picUrl)
                .addListener(object :
                    RequestListener<File> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<File>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("zfz", "$e")
                        emitter.onComplete()
                        return false
                    }

                    override fun onResourceReady(
                        resource: File,
                        model: Any?,
                        target: Target<File>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        emitter.onNext(
                            resource
                        )
                        emitter.onComplete()
                        return false
                    }

                }).preload()

        }).subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .subscribe {
                //获取到下载得到的图片，进行本地保存
                val destFile = createSaveFile(
                    context,
                    false,
                    "${System.currentTimeMillis()}.jpg",
                    "your_picture_save_path"
                )
                copy(it, destFile)
                refreshSystemPic(context, destFile)
                Looper.prepare()
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                Looper.loop()
            }
    }


    /**
     * 通知系统相册更新
     */
    private fun refreshSystemPic(context: Context, destFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertPicInAndroidQ(context, destFile)
        } else {
            //通知系统图库更新
            val value = ContentValues()
            value.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            value.put(MediaStore.Images.Media.DATA, destFile.absolutePath)
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
//            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                FileProvider.getUriForFile(
//                    context,
//                    "com.chiu.downloadpicformglide.fileProvider",
//                    destFile
//                )
//            } else {
//                Uri.fromFile(File(destFile.path))
//            }
//            context.sendBroadcast(
//                Intent(
//                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
//                    contentUri
//                )
//            )
        }
    }

    /**
     * 创建需要保存的文件
     * @param isUseExternalFilesDir 是否使用getExternalFilesDir,false为保存在sdcard根目录下
     * @param fileName 保存文件名
     * @param folderName 保存在sdcard根目录下的文件夹名（isUseExternalFilesDir=false时需要）
     */
    private fun createSaveFile(
        context: Context,
        isUseExternalFilesDir: Boolean,
        fileName: String,
        folderName: String? = ""
    ): File {
        val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath!!
        } else {
            if (isUseExternalFilesDir) {
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath!!
            } else {
                Environment.getExternalStorageDirectory().absolutePath
            }
        }
        return if (isUseExternalFilesDir) {
            File(filePath, fileName)
        } else {
            val file = File(filePath, folderName!!)
            if (!file.exists()) {
                file.mkdirs()
            }
            File(file, fileName)
        }
    }

    /**
     * 复制文件
     *
     * @param source 输入文件
     * @param target 输出文件
     */
    private fun copy(source: File?, target: File?) {
        var fileInputStream: FileInputStream? = null
        var fileOutputStream: FileOutputStream? = null
        try {
            fileInputStream = FileInputStream(source)
            fileOutputStream = FileOutputStream(target)
            val buffer = ByteArray(1024)
            while (fileInputStream.read(buffer) > 0) {
                fileOutputStream.write(buffer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                source?.delete()
                fileInputStream?.close()
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Android Q以后向系统相册插入图片
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertPicInAndroidQ(context: Context, insertFile: File) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DESCRIPTION, insertFile.name)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, insertFile.name)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.Media.TITLE, "Image.jpg")
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")

        val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver: ContentResolver = context.contentResolver
        val insertUri = resolver.insert(external, values)
        var inputStream: BufferedInputStream?
        var os: OutputStream? = null
        try {
            inputStream = BufferedInputStream(FileInputStream(insertFile))
            if (insertUri != null) {
                os = resolver.openOutputStream(insertUri)
            }
            if (os != null) {
                val buffer = ByteArray(1024 * 4)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    os.write(buffer, 0, len)
                }
                os.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            os?.close()
        }
    }

    //保存图片至app私有目录
    private fun saveBitmap2SelfDirectroy(
        context: Context,
        bitmap: Bitmap,
        file: File
    ) {
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        refreshSystemPic(context, file)
    }

}