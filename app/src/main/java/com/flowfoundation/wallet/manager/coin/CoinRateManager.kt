package com.flowfoundation.wallet.manager.coin

import android.text.format.DateUtils
import com.google.gson.annotations.SerializedName
import com.flowfoundation.wallet.cache.CacheManager
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.network.ApiService
import com.flowfoundation.wallet.network.retrofit
import com.flowfoundation.wallet.network.retrofitApi
import com.flowfoundation.wallet.page.token.detail.QuoteMarket
import com.flowfoundation.wallet.page.token.detail.getPricePair
import com.flowfoundation.wallet.page.token.detail.isUSDStableCoin
import com.flowfoundation.wallet.utils.getQuoteMarket
import com.flowfoundation.wallet.utils.ioScope
import com.flowfoundation.wallet.utils.logd
import com.flowfoundation.wallet.utils.uiScope
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object CoinRateManager {
    private val TAG = CoinRateManager::class.java.simpleName

    private var coinRateMap = ConcurrentHashMap<String, CoinRate>()

    private val listeners = CopyOnWriteArrayList<WeakReference<OnCoinRateUpdate>>()

    private val cache by lazy { CacheManager("COIN_RATE", CoinRateCacheData::class.java) }

    fun init() {
        ioScope {
            coinRateMap = ConcurrentHashMap<String, CoinRate>(cache.read()?.data.orEmpty())
        }
    }

    fun refresh() {
        ioScope { fetchCoinListRate(FlowCoinListManager.getEnabledCoinList()) }
    }

    fun addListener(callback: OnCoinRateUpdate) {
        if (listeners.firstOrNull { it.get() == callback } != null) {
            return
        }
        uiScope { this.listeners.add(WeakReference(callback)) }
    }

    fun coinRate(contractId: String) = coinRateMap[contractId]?.price

    fun fetchCoinRate(coin: FlowCoin) {
        ioScope {
            if (coin.isUSDStableCoin()) {
                dispatchListeners(coin, BigDecimal.ONE, 0f)
                return@ioScope
            }
            val cacheRate = coinRateMap[coin.contractId()]
            cacheRate?.let { dispatchListeners(coin, it.price, it.quoteChange) }
            if (cacheRate.isExpire()) {
                runCatching {
                    val market = QuoteMarket.fromMarketName(getQuoteMarket())
                    val coinPair = coin.getPricePair(market)

                    if (coinPair.isEmpty()) {
                        val apiService = retrofitApi().create(ApiService::class.java)
                        val tokenPriceResponse = apiService.getTokenPrices()
                        val tokenPriceList = tokenPriceResponse.data
                        tokenPriceList?.find {
                            if (WalletManager.isEVMAccountSelected()) {
                                coin.address == it.evmAddress
                            } else {
                                coin.contractName() == it.contractName
                            }
                        }?.let {
                            val rate = it.rateToUSD
                            updateCache(coin, rate, 0f)
                            dispatchListeners(coin, rate, 0f)
                        }
                        return@ioScope
                    }

                    val service = retrofit().create(ApiService::class.java)
                    val response = service.summary(market.value, coin.getPricePair(market))
                    val price = response.data.result.price.last
                    val quoteChange = response.data.result.price.change.percentage
                    updateCache(coin, price, quoteChange)
                    dispatchListeners(coin, price, quoteChange)
                }
            }
        }
    }

    fun fetchCoinListRate(list: List<FlowCoin>) {
        ioScope {
            val apiService = retrofitApi().create(ApiService::class.java)
            val tokenPriceResponse = apiService.getTokenPrices()
            val tokenPriceList = tokenPriceResponse.data
            list.forEach { coin ->
                if (coin.isUSDStableCoin()) {
                    dispatchListeners(coin, BigDecimal.ONE, 0f)
                    return@forEach
                }
                val cacheRate = coinRateMap[coin.contractId()]
                cacheRate?.let { dispatchListeners(coin, it.price, it.quoteChange) }
                if (cacheRate.isExpire()) {
                    runCatching {
                        val market = QuoteMarket.fromMarketName(getQuoteMarket())
                        val coinPair = coin.getPricePair(market)

                        if (coinPair.isEmpty()) {
                            tokenPriceList?.find {
                                if (WalletManager.isEVMAccountSelected()) {
                                    coin.address.equals(it.evmAddress, true)
                                } else {
                                    coin.contractName() == it.contractName
                                }
                            }?.let {
                                val rate = it.rateToUSD
                                updateCache(coin, rate, 0f)
                                dispatchListeners(coin, rate, 0f)
                            }
                            return@forEach
                        }
                        val service = retrofit().create(ApiService::class.java)
                        val response = service.summary(market.value, coin.getPricePair(market))
                        val price = response.data.result.price.last
                        val quoteChange = response.data.result.price.change.percentage
                        updateCache(coin, price, quoteChange)
                        dispatchListeners(coin, price, quoteChange)
                    }
                }
            }
        }
    }

    private fun CoinRate?.isExpire(): Boolean = this == null || System.currentTimeMillis() - updateTime > 30 * DateUtils.SECOND_IN_MILLIS

    private fun updateCache(coin: FlowCoin, price: BigDecimal, quoteChange: Float) {
        ioScope {
            coinRateMap[coin.contractId()] = CoinRate(coin.symbol, coin.contractId(), price, quoteChange, System.currentTimeMillis())
            cache.cache(CoinRateCacheData(coinRateMap))
        }
    }

    private fun dispatchListeners(coin: FlowCoin, price: BigDecimal, quoteChange: Float) {
        logd(TAG, "dispatchListeners ${coin.contractId()}:${price}")
        uiScope {
            listeners.removeAll { it.get() == null }
            listeners.forEach { it.get()?.onCoinRateUpdate(coin, price, quoteChange) }
        }
    }
}

interface OnCoinRateUpdate {
    fun onCoinRateUpdate(coin: FlowCoin, price: BigDecimal) {

    }

    fun onCoinRateUpdate(coin: FlowCoin, price: BigDecimal, quoteChange: Float) {
        onCoinRateUpdate(coin, price)
    }
}

private class CoinRateCacheData(
    @SerializedName("data")
    var data: Map<String, CoinRate>,
)

class CoinRate(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("contractId")
    val contractId: String? = "",
    @SerializedName("price")
    val price: BigDecimal,
    @SerializedName("quoteChange")
    val quoteChange: Float = 0f,
    @SerializedName("updateTime")
    val updateTime: Long,
)