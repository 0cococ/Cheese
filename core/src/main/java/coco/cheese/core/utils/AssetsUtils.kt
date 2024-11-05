package coco.cheese.core.utils

import android.content.Context
import android.util.Log
import coco.cheese.core.Env
import coco.cheese.core.interfaces.IBase
import com.elvishew.xlog.XLog
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.Stack

class AssetsUtils(private val env: Env) {
    fun read(path: String): String {
        val newPath = if (path.startsWith("/")) {
            path.substring(1)
        } else {
            path
        }
        if (!env.runTime.runMode) {
            val filePath = "${ASSETS_DIRECTORY()}/$newPath"
            return FilesUtils.get().read(filePath)?.joinToString(" ") ?: ""
        }
        if (isFile(newPath)) {
            return readFile(newPath)
        }
        return readFolder(newPath).joinToString(" ")

    }

    fun copy(path: String, destPath: String): Boolean {
        val newPath = if (path.startsWith("/")) {
            path.substring(1)
        } else {
            path
        }
        if (!env.runTime.runMode) {
            val filePath = "${ASSETS_DIRECTORY()}/$newPath"
            return FilesUtils.get().copy(filePath, destPath)
        }
        if (isFile(newPath)) {
            XLog.e("========1"+newPath+","+destPath)
            return copyFileToSD(newPath, destPath)
        }
        return copyFolderToSD(newPath, destPath)
    }

    private fun readFolder(path: String): Array<String> {
        val folderName_ = path

        val fileList = mutableListOf<String>()
        try {
            val stack = Stack<String>()
            stack.push(folderName_)

            while (stack.isNotEmpty()) {
                val currentPath = stack.pop()

                val list = env.context.assets.list(currentPath) ?: continue

                for (item in list) {
                    val subPath = if (currentPath.isEmpty()) item else "$currentPath/$item"

                    try {
                        env.context.assets.open(subPath).use { _ ->
                            // 如果成功打开文件流，说明是文件而非目录
                            fileList.add(subPath)
                        }
                    } catch (e: IOException) {
                        // 报错说明是目录，将子目录路径入栈，以便后续处理
                        stack.push(subPath)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return fileList.toTypedArray()
    }

    private fun readFile(fileName: String): String {
        val fileName_ = fileName

        return env.context.assets.open(fileName_).use { inputStream ->
            inputStream.readBytes().toString(Charsets.UTF_8)
        }

    }

    fun copyFileToSD(fileName: String, destPath: String): Boolean {
        val assetManager = env.context.assets
        return try {
            val inputStream = assetManager.open(fileName)

            // 检查目标路径是否是目录
            val destFile = if (File(destPath).isDirectory) {
                // 从文件名中提取实际文件名
                val actualFileName = fileName.substringAfterLast("/")
                File(destPath, actualFileName) // 在目录下创建目标文件
            } else {
                File(destPath) // 如果不是目录，直接创建文件对象
            }
            XLog.e(destFile)

            // 确保目标文件存在
            if (!destFile.exists()) {
                destFile.createNewFile() // 创建文件
            }

            // 写入文件
            FileOutputStream(destFile).use { outputStream ->
                inputStream.use { input ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    private fun copyFolderToSD(sourceFolder: String, destFolder: String): Boolean {
        return try {
            val assetManager = env.context.assets
            val files = assetManager.list(sourceFolder) ?: return false

            // 从源文件夹获取目标文件夹的名称
            val targetFolderName = File(sourceFolder).name
            val targetFolder = File(destFolder, targetFolderName).apply {
                if (!exists()) {
                    mkdirs()
                }
            }

            for (filename in files) {
                val fullPath = "$sourceFolder/$filename" // 源文件的完整路径

                if (isFolder(fullPath)) {
                    Log.d("CopyFolder", "Copying folder: $fullPath")
                    // 递归调用复制子文件夹
                    copyFolderToSD(fullPath, targetFolder.absolutePath) // 传递目标文件夹的绝对路径
                } else {
                    Log.d("CopyFolder", "Copying file: $fullPath to ${File(targetFolder, filename).absolutePath}")
                    // 复制文件
                    assetManager.open(fullPath).use { inputStream ->
                        File(targetFolder, filename).outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    fun isFolder(folderPath: String): Boolean {
        val newPath = if (folderPath.startsWith("/")) {
            folderPath.substring(1)
        } else {
            folderPath
        }
        if (!env.runTime.runMode) {
            return FilesUtils.get().isFolder("${ASSETS_DIRECTORY()}/$newPath")
        }

        val assetManager = env.context.assets
        try {
            val files = assetManager.list(folderPath)
            return !files.isNullOrEmpty()
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
        return false
    }




    fun isFile(filePath: String): Boolean {
        val newPath = if (filePath.startsWith("/")) {
            filePath.substring(1)
        } else {
            filePath
        }
        if (!env.runTime.runMode) {
            return FilesUtils.get().isFile("${ASSETS_DIRECTORY()}/$newPath")
        }

        return try {
            val inputStream = env.context.assets.open(filePath)
            inputStream.close()
            true
        } catch (e: IOException) {
            false
        }
    }


    companion object : IBase {
        private var instanceWeak: WeakReference<AssetsUtils>? = null
        private var instance: AssetsUtils? = null
        private val lock = Any()
        override fun get(env: Env, examine: Boolean): AssetsUtils {
            if (this.instance == null || !examine) {
                synchronized(this.lock) {
                    this.instance = AssetsUtils(env)
                }
            }
            return this.instance!!
        }

        override fun getWeak(env: Env, examine: Boolean): AssetsUtils {
            if (this.instanceWeak?.get() == null || !examine) {
                synchronized(this.lock) {
                    this.instanceWeak = WeakReference(AssetsUtils(env))
                }
            }
            return this.instanceWeak?.get()!!
        }

    }
}
