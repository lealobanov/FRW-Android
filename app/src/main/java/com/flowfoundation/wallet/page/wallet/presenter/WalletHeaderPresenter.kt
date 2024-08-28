package com.flowfoundation.wallet.page.wallet.presenter

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.flowfoundation.wallet.R
import com.flowfoundation.wallet.base.presenter.BasePresenter
import com.flowfoundation.wallet.base.recyclerview.BaseViewHolder
import com.flowfoundation.wallet.databinding.LayoutWalletCoordinatorHeaderBinding
import com.flowfoundation.wallet.manager.app.isMainnet
import com.flowfoundation.wallet.manager.app.isPreviewnet
import com.flowfoundation.wallet.manager.app.isTestnet
import com.flowfoundation.wallet.manager.coin.FlowCoinListManager
import com.flowfoundation.wallet.manager.coin.TokenStateManager
import com.flowfoundation.wallet.manager.config.AppConfig
import com.flowfoundation.wallet.manager.notification.WalletNotificationManager
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.manager.walletconnect.WalletConnect
import com.flowfoundation.wallet.manager.walletconnect.getWalletConnectPendingRequests
import com.flowfoundation.wallet.page.browser.openBrowser
import com.flowfoundation.wallet.page.notification.model.DisplayType
import com.flowfoundation.wallet.page.notification.model.Priority
import com.flowfoundation.wallet.page.notification.model.Type
import com.flowfoundation.wallet.page.notification.model.WalletNotification
import com.flowfoundation.wallet.page.profile.subpage.walletconnect.session.model.PendingRequestModel
import com.flowfoundation.wallet.page.receive.ReceiveActivity
import com.flowfoundation.wallet.page.send.transaction.TransactionSendActivity
import com.flowfoundation.wallet.page.staking.openStakingPage
import com.flowfoundation.wallet.page.swap.SwapActivity
import com.flowfoundation.wallet.page.token.addtoken.AddTokenActivity
import com.flowfoundation.wallet.page.wallet.WalletFragmentViewModel
import com.flowfoundation.wallet.page.wallet.dialog.SwapDialog
import com.flowfoundation.wallet.page.wallet.model.WalletHeaderModel
import com.flowfoundation.wallet.utils.*
import com.flowfoundation.wallet.utils.extensions.dp2px
import com.flowfoundation.wallet.utils.extensions.gone
import com.flowfoundation.wallet.utils.extensions.res2String
import com.flowfoundation.wallet.utils.extensions.setVisible
import com.flowfoundation.wallet.utils.extensions.visible
import com.flowfoundation.wallet.wallet.toAddress
import java.util.Date

class WalletHeaderPresenter(
    private val view: View,
) : BaseViewHolder(view), BasePresenter<WalletHeaderModel?> {

    private val binding by lazy { LayoutWalletCoordinatorHeaderBinding.bind(view) }

    private val viewModel by lazy { ViewModelProvider(findActivity(view) as FragmentActivity)[WalletFragmentViewModel::class.java] }

    private val activity by lazy { findActivity(view) as? FragmentActivity }

    init {
        binding.cvStake.setVisible(isMainnet())
    }

    @SuppressLint("SetTextI18n")
    override fun bind(model: WalletHeaderModel?) {
        binding.root.setVisible(model != null)
        model ?: return

        with(binding) {
            uiScope {
                val isHideBalance = isHideWalletBalance()
                tvBalance.diffSetText(
                    if (isHideBalance) "****" else model.balance.formatPrice(
                        includeSymbol = true
                    )
                )
                ivHide.setImageResource(if (isHideBalance) R.drawable.ic_eye_off else R.drawable.ic_eye_on)
            }

            val count =
                FlowCoinListManager.coinList().count { TokenStateManager.isTokenAdded(it.address) }
            tvTokenCount.text = view.context.getString(R.string.token_count, count)

            cvSend.setOnClickListener { TransactionSendActivity.launch(view.context) }
            cvReceive.setOnClickListener { ReceiveActivity.launch(view.context) }
            tvAddress.text = shortenEVMString(WalletManager.selectedWalletAddress().toAddress())
            ivCopy.setOnClickListener {
                copyAddress(
                    WalletManager.selectedWalletAddress().toAddress()
                )
            }
            if (WalletManager.isChildAccountSelected()) {
                llSend.changeLayoutParams(LinearLayoutCompat.HORIZONTAL, 44f)
                llReceive.changeLayoutParams(LinearLayoutCompat.HORIZONTAL, 44f)
                cvSwap.gone()
                cvStake.gone()
                cvBuy.gone()
                ivAddToken.gone()
            } else {
                llSend.changeLayoutParams(LinearLayoutCompat.VERTICAL, 64f)
                llReceive.changeLayoutParams(LinearLayoutCompat.VERTICAL, 64f)
                ivAddToken.setOnClickListener { AddTokenActivity.launch(view.context) }
                cvSwap.setOnClickListener {
                    if (AppConfig.isInAppSwap()) {
                        SwapActivity.launch(view.context)
                    } else {
                        activity?.let {
                            openBrowser(
                                it,
                                "https://${if (isTestnet() || isPreviewnet()) "demo" else "app"}" +
                                        ".increment.fi/swap"
                            )
                        }
                    }
                }
                cvStake.setOnClickListener { openStakingPage(view.context) }
                cvBuy.setOnClickListener { activity?.let { SwapDialog.show(it.supportFragmentManager) } }
                cvBuy.setVisible(AppConfig.isInAppBuy())
                cvSwap.setVisible(AppConfig.isInAppSwap())
                cvStake.visible()
                ivAddToken.setVisible(WalletManager.isEVMAccountSelected().not())
            }

            with(cvSend) {
                if (WalletManager.isChildAccountSelected()) {
                    isEnabled = false
                    alpha = 0.5f
                } else {
                    isEnabled = true
                    alpha = 1f
                }
            }

            ivHide.setOnClickListener {
                uiScope {
                    setHideWalletBalance(!isHideWalletBalance())
                    bind(model)
                    viewModel.onBalanceHideStateUpdate()
                }
            }

            bindPendingRequest()
        }
    }

    private fun LinearLayoutCompat.changeLayoutParams(newOrientation: Int, newHeight: Float) {
        orientation = newOrientation
        layoutParams = layoutParams.apply {
            height = newHeight.dp2px().toInt()
        }
    }

    private fun copyAddress(text: String) {
        textToClipboard(text)
        Toast.makeText(view.context, R.string.copy_address_toast.res2String(), Toast.LENGTH_SHORT)
            .show()
    }

    private fun bindPendingRequest() {
        logd("notification", "bindPendingRequest")

        ioScope {
            val sessions = WalletConnect.get().sessions()
            val requests = getWalletConnectPendingRequests().map { request ->
                PendingRequestModel(
                    request = request,
                    metadata = sessions.firstOrNull { request.topic == it.topic }?.metaData
                )
            }.filter { it.metadata != null }
            requests.firstOrNull()?.let {
                logd("notification", "pendingRequest::$it")
                if (WalletNotificationManager.alreadyExist(it.request.requestId.toString())) {
                    return@ioScope
                }
                WalletNotificationManager.addNotification(
                    WalletNotification(
                        id = it.request.requestId.toString(),
                        icon = it.metadata?.icons?.firstOrNull(),
                        title = it.metadata?.name.orEmpty(),
                        body = "View More",
                        priority = Priority.URGENT,
                        type = Type.PENDING_REQUEST,
                        expiryTime = Date(),
                        displayType = DisplayType.CLICK
                    )
                )
            }
        }
    }

    private fun TextView.diffSetText(text: String?) {
        if (text != this.text.toString()) {
            this.text = text
        }
    }
}
