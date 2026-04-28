package com.enon.writingai.core.permissions

import android.Manifest
import android.os.Build

object PermissionDescriptor {
    val cameraPermission: String = Manifest.permission.CAMERA
    val cameraPermissions = arrayOf(cameraPermission)

    val galleryPermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
}
