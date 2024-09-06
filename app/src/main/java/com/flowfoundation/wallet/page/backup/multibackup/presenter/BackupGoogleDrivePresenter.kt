package com.flowfoundation.wallet.page.backup.multibackup.presenter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.flowfoundation.wallet.R
import com.flowfoundation.wallet.base.presenter.BasePresenter
import com.flowfoundation.wallet.databinding.FragmentBackupGoogleDriveBinding
import com.flowfoundation.wallet.manager.drive.GoogleDriveAuthActivity
import com.flowfoundation.wallet.page.backup.multibackup.model.BackupGoogleDriveState
import com.flowfoundation.wallet.page.backup.multibackup.viewmodel.BackupGoogleDriveViewModel
import com.flowfoundation.wallet.page.backup.multibackup.viewmodel.BackupGoogleDriveWithPinViewModel
import com.flowfoundation.wallet.utils.extensions.res2String
import com.flowfoundation.wallet.utils.extensions.res2color
import com.flowfoundation.wallet.utils.getPinCode

class BackupGoogleDrivePresenter(
    private val fragment: Fragment,
    private val binding: FragmentBackupGoogleDriveBinding
) : BasePresenter<BackupGoogleDriveState> {

    private val viewModel by lazy {
        ViewModelProvider(fragment)[BackupGoogleDriveViewModel::class.java]
    }

    private val withPinViewModel by lazy {
        ViewModelProvider(fragment.requireParentFragment())[BackupGoogleDriveWithPinViewModel::class.java]
    }

    private var currentState = BackupGoogleDriveState.CREATE_BACKUP

    init {
        with(binding) {
            clStatusLayout.visibility = View.GONE
            btnNext.setOnClickListener {
                if (btnNext.isProgressVisible()) {
                    return@setOnClickListener
                }
                btnNext.setProgressVisible(true)
                when (currentState) {
                    BackupGoogleDriveState.CREATE_BACKUP -> viewModel.createBackup()
                    BackupGoogleDriveState.UPLOAD_BACKUP -> {
                        viewModel.uploadToChain()
                    }
                    BackupGoogleDriveState.UPLOAD_BACKUP_FAILURE -> {
                        viewModel.uploadToChain()
                    }
                    BackupGoogleDriveState.REGISTRATION_KEY_LIST -> {}
                    BackupGoogleDriveState.NETWORK_ERROR -> viewModel.registrationKeyList()
                    BackupGoogleDriveState.BACKUP_SUCCESS -> withPinViewModel.backupFinish()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(model: BackupGoogleDriveState) {
        currentState = model
        with(binding) {
            when (model) {
                BackupGoogleDriveState.CREATE_BACKUP -> {
                    btnNext.setProgressVisible(false)
                    tvOptionTitle.text = fragment.requireContext().getString(R.string.backup_step_google_drive, (withPinViewModel.getCurrentIndex() + 1))
                    clStatusLayout.visibility = View.GONE
                    btnNext.text = R.string.create_backup.res2String()
                }
                BackupGoogleDriveState.UPLOAD_BACKUP -> {
                    btnNext.setProgressVisible(false)
                    tvOptionTitle.text = R.string.upload_backup.res2String()
                    clStatusLayout.visibility = View.VISIBLE
                    viewUpload.backgroundTintList = ColorStateList.valueOf(R.color.text_2.res2color())
                    tvUpload.setTextColor(R.color.text_2.res2color())
                    viewLine.setBackgroundColor(R.color.text_3.res2color())
                    viewRegistration.backgroundTintList = ColorStateList.valueOf(R.color.text_3.res2color())
                    tvRegistration.setTextColor(R.color.text_3.res2color())
                    btnNext.text = R.string.upload_backup.res2String()
                }
                BackupGoogleDriveState.UPLOAD_BACKUP_FAILURE -> {
                    btnNext.setProgressVisible(false)
                    tvOptionTitle.text = R.string.upload_backup.res2String()
                    clStatusLayout.visibility = View.VISIBLE
                    viewUpload.backgroundTintList = ColorStateList.valueOf(R.color.accent_red.res2color())
                    tvUpload.setTextColor(R.color.accent_red.res2color())
                    viewLine.setBackgroundColor(R.color.text_3.res2color())
                    viewRegistration.backgroundTintList = ColorStateList.valueOf(R.color.text_3.res2color())
                    tvRegistration.setTextColor(R.color.text_3.res2color())
                    btnNext.text = R.string.upload_again.res2String()
                }
                BackupGoogleDriveState.REGISTRATION_KEY_LIST -> {
                    tvOptionTitle.text = R.string.upload_backup.res2String()
                    clStatusLayout.visibility = View.VISIBLE
                    viewUpload.backgroundTintList = ColorStateList.valueOf(R.color.text_2.res2color())
                    tvUpload.setTextColor(R.color.text_2.res2color())
                    viewLine.setBackgroundColor(R.color.text_2.res2color())
                    viewRegistration.backgroundTintList = ColorStateList.valueOf(R.color.text_2.res2color())
                    tvRegistration.setTextColor(R.color.text_2.res2color())
                    btnNext.text = R.string.upload_backup.res2String()
                }
                BackupGoogleDriveState.NETWORK_ERROR -> {
                    btnNext.setProgressVisible(false)
                    tvOptionTitle.text = R.string.network_connect_lost.res2String()
                    clStatusLayout.visibility = View.GONE
                    btnNext.text = R.string.try_to_connect.res2String()
                }
                BackupGoogleDriveState.BACKUP_SUCCESS -> {
                    btnNext.setProgressVisible(false)
                    tvOptionTitle.text = R.string.backup_uploaded.res2String()
                    clStatusLayout.visibility = View.VISIBLE
                    btnNext.text = R.string.next.res2String()
                }
            }
        }
    }

    fun uploadMnemonic(mnemonic: String) {
        if (getPinCode().isBlank()) {
            withPinViewModel.backToPinCode()
            return
        }
        GoogleDriveAuthActivity.multiBackupMnemonic(fragment.requireContext(), mnemonic)
    }
}