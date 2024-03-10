package io.outblock.lilico.page.restore.multirestore.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.outblock.lilico.databinding.FragmentRestorePinCodeBinding
import io.outblock.lilico.page.restore.multirestore.presenter.RestorePinCodePresenter


class RestorePinCodeFragment: Fragment() {
    private lateinit var binding: FragmentRestorePinCodeBinding
    private lateinit var presenter: RestorePinCodePresenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRestorePinCodeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = RestorePinCodePresenter(this, binding)
    }
}