package com.eliya.callguard.screening

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** What the user still needs to grant for the app to work fully. */
data class SetupStatus(
    val screeningRole: Boolean = false,
    val contacts: Boolean = false,
    val callControl: Boolean = false,
    val notifications: Boolean = false,
) {
    /** The minimum needed to filter calls from saved contacts. */
    val isReady: Boolean get() = screeningRole && contacts
    val isComplete: Boolean get() = isReady && callControl && notifications

    companion object {
        fun read(context: Context): SetupStatus {
            val role = context.getSystemService(RoleManager::class.java)
            return SetupStatus(
                screeningRole = role?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true,
                contacts = granted(context, Manifest.permission.READ_CONTACTS),
                callControl = HangUpService.hasCallControl(context),
                notifications = Build.VERSION.SDK_INT < 33 ||
                    granted(context, Manifest.permission.POST_NOTIFICATIONS),
            )
        }

        private fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
