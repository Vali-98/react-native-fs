package com.chatterui.reactnativelocaldownload

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.Promise
import com.facebook.react.module.annotations.ReactModule

import android.content.ContentValues
import android.content.ContentResolver
import android.net.Uri
import android.system.Os
import android.content.Intent
import android.provider.MediaStore
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLConnection

@ReactModule(name = ReactNativeLocalDownloadModule.NAME)
class ReactNativeLocalDownloadModule(private val reactContext: ReactApplicationContext) :
  NativeReactNativeLocalDownloadSpec(reactContext) {

  override fun getName(): String {
    return NAME
  }

  override fun getContentFd(contentUri: String, promise: Promise) {
    try {
        val uri = Uri.parse(contentUri)

        val pfd = reactContext.contentResolver
        .openFileDescriptor(uri, "r")
        ?: run {
            promise.reject("FD_OPEN_FAILED", "Unable to open content URI")
            return
        }

        val fd = pfd.detachFd()
        promise.resolve("/proc/self/fd/$fd")

    } catch (e: Exception) {
        promise.reject(
        "GET_FD_ERROR",
        "Failed to get detached FD: ${e.message}",
        e
        )
    }
  }

  override fun closeFd(fdOrPath: String, promise: Promise) {
    try {
        val fdInt = when {
        fdOrPath.startsWith("/proc/") ->
            fdOrPath.substringAfterLast("/").toInt()
        else ->
            fdOrPath.toInt()
        }
        ParcelFileDescriptor.adoptFd(fdInt).close()
        promise.resolve(true)
    } catch (e: Exception) {
        promise.reject(
        "FD_CLOSE_ERROR",
        "Failed to close FD: ${e.message}",
        e
        )
    }
  }

    override fun persistContentPermission(uriString: String, promise: Promise) {
    try {
        val uri = Uri.parse(uriString)

        val flags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        reactContext.contentResolver
        .takePersistableUriPermission(uri, flags)

        promise.resolve(true)

    } catch (e: SecurityException) {
        promise.reject(
        "PERSIST_PERMISSION_DENIED",
        "Persistable permission not granted for this URI",
        e
        )
    } catch (e: Exception) {
        promise.reject(
        "PERSIST_PERMISSION_ERROR",
        e.message,
        e
        )
    }
    }


  override fun localDownload(uri: String, promise: Promise) {
    try {
      val inputFile = File(uri)
      if (!inputFile.exists()) {
        promise.reject("FILE_NOT_FOUND", "File does not exist at path: $uri")
        return
      }
  
      val fileName = inputFile.name
      val mimeType = URLConnection.guessContentTypeFromName(inputFile.name) ?: "application/octet-stream"
      val resolver : ContentResolver = reactContext.contentResolver
      val uniqueName = getUniqueFileName(resolver, fileName)
      val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, uniqueName)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
  
      val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      val itemUri = resolver.insert(collection, contentValues)
  
      if (itemUri == null) {
        promise.reject("SAVE_ERROR", "Failed to create destination file in MediaStore.")
        return
      }
  
      resolver.openOutputStream(itemUri)?.use { outputStream ->
        inputFile.inputStream().use { inputStream ->
          inputStream.copyTo(outputStream)
        }
      }
  
      // Mark the item as not pending so it's visible to user
      contentValues.clear()
      contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
      resolver.update(itemUri, contentValues, null, null)
  
      promise.resolve(itemUri.toString())
    } catch (e: Exception) {
      promise.reject("DOWNLOAD_ERROR", "Failed to save file via MediaStore: ${e.message}", e)
    }
  }
  
  private fun getUniqueFileName(resolver: ContentResolver, baseName: String): String {
    var name = baseName
    val nameWithoutExtension = File(baseName).nameWithoutExtension
    val extension = File(baseName).extension
    var index = 1
  
    val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
  
    val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
    val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
    val selectionArgs = arrayOf(name)
  
    while (resolver.query(collection, projection, selection, selectionArgs, null)?.use { it.moveToFirst() } == true) {
      name = if (extension.isNotEmpty()) {
        "$nameWithoutExtension ($index).$extension"
      } else {
        "$nameWithoutExtension ($index)"
      }
      selectionArgs[0] = name
      index++
    }
  
    return name
  }

  companion object {
    const val NAME = "ReactNativeLocalDownload"
  }
}
