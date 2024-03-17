package com.flowfoundation.wallet.page.backup.model

import com.flowfoundation.wallet.R


enum class BackupType(val index: Int, val displayName: String, val iconRes: Int, val keyIconRes: Int) {
    GOOGLE_DRIVE(0, "Backup - Google Drive", R.drawable.ic_backup_google_drive, R.drawable.ic_key_type_google_drive),
    ICLOUD(1, "Backup - iCloud", R.drawable.ic_backup_icloud, R.drawable.ic_key_type_icloud),
    MANUAL(2, "Backup - Manual", R.drawable.ic_backup_recovery_phrase, R.drawable.ic_key_type_recovery_phrase),
    PASSKEY(3, "Backup - Passkey", R.drawable.ic_backup_passkey, R.drawable.ic_key_type_passkey);

    companion object {

        @JvmStatic
        fun getBackupName(type: Int): String {
            return values().firstOrNull { it.index == type }?.displayName ?: ""
        }

        @JvmStatic
        fun getBackupIcon(type: Int): Int {
            return values().firstOrNull { it.index == type }?.iconRes ?: R.drawable.ic_settings_backup
        }

        @JvmStatic
        fun getBackupType(type: Int): BackupType? {
            return values().firstOrNull { it.index == type }
        }

        @JvmStatic
        fun getBackupKeyIcon(type: Int): Int {
            return values().firstOrNull {it.index == type}?.keyIconRes ?: R.drawable.ic_key_type_passkey
        }
    }
}