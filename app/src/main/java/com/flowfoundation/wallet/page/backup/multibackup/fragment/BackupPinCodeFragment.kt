package com.flowfoundation.wallet.page.backup.multibackup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.flowfoundation.wallet.databinding.FragmentBackupPinCodeBinding
import com.flowfoundation.wallet.page.backup.multibackup.presenter.BackupPinCodePresenter

class BackupPinCodeFragment: Fragment() {
    private lateinit var binding: FragmentBackupPinCodeBinding
    private lateinit var presenter: BackupPinCodePresenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBackupPinCodeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        presenter = BackupPinCodePresenter(this, binding)
    }
}