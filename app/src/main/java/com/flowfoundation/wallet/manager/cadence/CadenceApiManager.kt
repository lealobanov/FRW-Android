package com.flowfoundation.wallet.manager.cadence

import com.flowfoundation.wallet.manager.app.chainNetwork
import com.flowfoundation.wallet.network.ApiService
import com.flowfoundation.wallet.network.retrofitApi
import com.flowfoundation.wallet.utils.Env
import com.flowfoundation.wallet.utils.NETWORK_MAINNET
import com.flowfoundation.wallet.utils.NETWORK_PREVIEWNET
import com.flowfoundation.wallet.utils.NETWORK_TESTNET
import com.flowfoundation.wallet.utils.ioScope
import com.flowfoundation.wallet.utils.read
import com.flowfoundation.wallet.utils.readTextFromAssets
import com.flowfoundation.wallet.utils.saveToFile
import com.google.gson.Gson
import okio.ByteString.Companion.decodeBase64
import java.io.File


object CadenceApiManager {

    private const val LOCAL_CADENCE_FILE_NAME = "local_cadence.json"
    private const val ASSETS_CADENCE_FILE_PATH = "config/cadence_api.json"
    private var cadenceApi: CadenceScriptData? = null

    fun init() {
        loadCadenceFromLocal()
    }

    private fun loadCadenceFromLocal() {
        try {
            val file = File(Env.getApp().filesDir, LOCAL_CADENCE_FILE_NAME)
            cadenceApi = if (file.exists()) {
                val fileData = Gson().fromJson(file.read(), CadenceScriptResponse::class.java)
                fileData.data
            } else {
                val assetsData = Gson().fromJson(
                    readTextFromAssets(ASSETS_CADENCE_FILE_PATH),
                    CadenceScriptResponse::class.java
                )
                assetsData.data
            }
//            fetchCadenceFromNetwork()
        } catch (e: Exception) {
            e.printStackTrace()
//            fetchCadenceFromNetwork()
        }
    }

    private fun saveCadenceToLocal(json: String) {
        json.saveToFile(File(Env.getApp().filesDir, LOCAL_CADENCE_FILE_NAME))
    }

    private fun fetchCadenceFromNetwork() {
        ioScope {
            try {
                val response = retrofitApi().create(ApiService::class.java).getCadenceScript()
                if (response.data == null) {
                    return@ioScope
                }
                cadenceApi = response.data
                saveCadenceToLocal(Gson().toJson(response))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCadenceScript(): CadenceScript? {
        return when (chainNetwork()) {
            NETWORK_TESTNET -> cadenceApi?.scripts?.testnet
            NETWORK_PREVIEWNET -> cadenceApi?.scripts?.previewnet
            else -> cadenceApi?.scripts?.mainnet
        }
    }

    fun getCadenceBasicScript(method: String): String {
        return getCadenceScript()?.basic?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceAccountScript(method: String): String {
        return getCadenceScript()?.account?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceCollectionScript(method: String): String {
        return getCadenceScript()?.collection?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceContractScript(method: String): String {
        return getCadenceScript()?.contract?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceDomainScript(method: String): String {
        return getCadenceScript()?.domain?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceFTScript(method: String): String {
        return getCadenceScript()?.ft?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceHybridCustodyScript(method: String): String {
        return getCadenceScript()?.hybridCustody?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceStakingScript(method: String): String {
        return getCadenceScript()?.staking?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceStorageScript(method: String): String {
        return getCadenceScript()?.storage?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceSwitchboardScript(method: String): String {
        return getCadenceScript()?.switchboard?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceEVMScript(method: String): String {
        return getCadenceScript()?.evm?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceNFTScript(method: String): String {
        return getCadenceScript()?.nft?.get(method)?.decodeBase64()?.utf8() ?: ""
    }

    fun getCadenceSwapScript(method: String): String {
        return (getCadenceScript()?.swap?.get(method) as? String)?.decodeBase64()?.utf8() ?: ""
    }
}
