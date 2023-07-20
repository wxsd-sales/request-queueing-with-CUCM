package com.ciscowebex.androidsdk.kitchensink.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionsHelper(private val context: Context) {

    fun hasCameraPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA)
    }

    fun hasMicrophonePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO)
    }

    fun hasPhoneStatePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
    }

    fun hasReadStoragePermission(): Boolean {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun checkSelfPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val PERMISSIONS_CALLING_REQUEST = 0
        const val PERMISSIONS_STORAGE_REQUEST = 1
        const val PERMISSIONS_CAMERA_REQUEST = 2

        fun permissionsForCalling(): Array<String> {
            return arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
        }

        fun permissionForCamera(): Array<String> {
            return arrayOf(Manifest.permission.CAMERA)
        }

        fun permissionForStorage(): Array<String> {
            return arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        fun resultForCallingPermissions(permissions: Array<String>, grantResults: IntArray): Boolean {
            var result = true

            for (permission in permissions) {
                result = result and checkPermissionsResults(permission, permissions, grantResults)
            }

            return result
        }

        private fun checkPermissionsResults(permissionRequested: String, permissions: Array<String>, grantResults: IntArray): Boolean {
            for (index in permissions.indices) {
                val permission = permissions[index]
                val grantResult = grantResults[index]

                if (permissionRequested == permission) {
                    return grantResult == PackageManager.PERMISSION_GRANTED
                }
            }

            return false
        }
    }
}
