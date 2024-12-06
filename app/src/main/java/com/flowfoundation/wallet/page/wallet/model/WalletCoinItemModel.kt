package com.flowfoundation.wallet.page.wallet.model

import com.flowfoundation.wallet.manager.coin.FlowCoin
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class WalletCoinItemModel(
    @SerializedName("coin")
    val coin: FlowCoin,
    @SerializedName("address")
    val address: String,
    @SerializedName("balance")
    val balance: BigDecimal,
    @SerializedName("coinRate")
    val coinRate: BigDecimal,
    @SerializedName("isHideBalance")
    val isHideBalance: Boolean = false,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("isStaked")
    val isStaked: Boolean = false,
    @SerializedName("stakeAmount")
    val stakeAmount: Float,
    @SerializedName("quoteChange")
    val quoteChange: Float = 0f,
)