package com.flowfoundation.wallet.manager.nft

import com.google.gson.annotations.SerializedName
import com.flowfoundation.wallet.cache.nftCollectionStateCache
import com.flowfoundation.wallet.manager.config.NftCollection
import com.flowfoundation.wallet.manager.config.NftCollectionConfig
import com.flowfoundation.wallet.manager.flowjvm.cadenceCheckNFTListEnabled
import com.flowfoundation.wallet.manager.flowjvm.cadenceNftCheckEnabled
import com.flowfoundation.wallet.manager.wallet.WalletManager
import com.flowfoundation.wallet.utils.ioScope
import com.flowfoundation.wallet.utils.logd
import com.flowfoundation.wallet.utils.logw
import com.flowfoundation.wallet.utils.uiScope
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

object NftCollectionStateManager {
    private val TAG = NftCollectionStateManager::class.java.simpleName

    private val tokenStateList = CopyOnWriteArrayList<NftCollectionState>()
    private val listeners = CopyOnWriteArrayList<WeakReference<NftCollectionStateChangeListener>>()

    fun reload() {
        ioScope {
            tokenStateList.clear()
            tokenStateList.addAll(nftCollectionStateCache().read()?.stateList ?: emptyList())
        }
    }

    fun fetchState(onFinish: (() -> Unit)? = null) {
        if (WalletManager.isEVMAccountSelected()) {
            return
        }
        ioScope { fetchStateSync(onFinish) }
    }

    private suspend fun fetchStateSync(onFinish: (() -> Unit)? = null) {
        val collectionList = NftCollectionConfig.list()

        val collectionMap = cadenceCheckNFTListEnabled()
        logd(TAG, "enable nft list:: ${collectionMap.toString()}")

        if (collectionMap.isNullOrEmpty()) {
            logw(TAG, "fetch error")
            return
        }
        collectionList.forEach { collection ->
            val isEnable = collectionMap.getOrDefault(collection.contractId(), false)
            val oldState = tokenStateList.firstOrNull { it.address == collection.address }
            tokenStateList.remove(oldState)
            tokenStateList.add(NftCollectionState(collection.name, collection.address.orEmpty(), isEnable))
            if (oldState?.isAdded != isEnable) {
                dispatchListeners(collection, isEnable)
            }
        }
        nftCollectionStateCache().cache(NftCollectionStateCache(tokenStateList.toList()))
        onFinish?.invoke()
    }

    suspend fun fetchStateSingle(collection: NftCollection, cache: Boolean = false) {
        val isEnable = cadenceNftCheckEnabled(collection)
        if (isEnable != null) {
            val oldState = tokenStateList.firstOrNull { it.name == collection.name }
            tokenStateList.remove(oldState)
            tokenStateList.add(NftCollectionState(collection.name, collection.address.orEmpty(), isEnable))
            if (oldState?.isAdded != isEnable) {
                dispatchListeners(collection, isEnable)
            }
        }
        if (cache) {
            nftCollectionStateCache().cache(NftCollectionStateCache(tokenStateList.toList()))
        }
    }

    fun isTokenAdded(tokenAddress: String?) = tokenStateList.firstOrNull { it.address == tokenAddress }?.isAdded ?: false

    fun addListener(callback: NftCollectionStateChangeListener) {
        uiScope { this.listeners.add(WeakReference(callback)) }
    }

    private fun dispatchListeners(collection: NftCollection, isEnable: Boolean) {
        logd(TAG, "${collection.name} isEnable:$isEnable")
        uiScope {
            listeners.removeAll { it.get() == null }
            listeners.toList().forEach { it.get()?.onNftCollectionStateChange(collection, isEnable) }
        }
    }

    fun clear() {
        tokenStateList.clear()
        nftCollectionStateCache().clear()
    }
}

interface NftCollectionStateChangeListener {
    fun onNftCollectionStateChange(collection: NftCollection, isEnable: Boolean)
}

class NftCollectionStateCache(
    @SerializedName("stateList")
    val stateList: List<NftCollectionState> = emptyList(),
)

class NftCollectionState(
    @SerializedName("name")
    val name: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("isAdded")
    val isAdded: Boolean,
)