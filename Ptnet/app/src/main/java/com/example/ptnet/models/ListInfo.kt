package com.example.ptnet.models

import com.example.ptnet.R
import androidx.collection.ArraySet
import com.example.ptnet.PCAPdroid
import com.example.ptnet.models.MatchList.RuleType
import com.example.ptnet.services.CaptureService
import java.util.Arrays


class ListInfo {
    lateinit var mType: Type


    enum class Type {
        VISUALIZATION_MASK,
        MALWARE_WHITELIST,
        BLOCKLIST,
        FIREWALL_WHITELIST,
        DECRYPTION_LIST
    }

    constructor(tp: Type) {
        mType = tp;
    }

    fun getType(): Type {
        return mType
    }

    fun getList(): MatchList? {
        return when (mType) {
            Type.VISUALIZATION_MASK -> PCAPdroid().getInstance().getVisualizationMask()
            Type.MALWARE_WHITELIST -> PCAPdroid().getInstance().getMalwareWhitelist()
            Type.BLOCKLIST -> PCAPdroid().getInstance().getBlocklist()
            Type.FIREWALL_WHITELIST -> PCAPdroid().getInstance().getFirewallWhitelist()
            Type.DECRYPTION_LIST -> PCAPdroid().getInstance().getDecryptionList()
            else -> {
                assert(false)
                null
            }
        }
    }

    fun getTitle(): Int {
        return when (mType) {
            Type.VISUALIZATION_MASK -> R.string.hidden_connections_rules
            Type.MALWARE_WHITELIST -> R.string.malware_whitelist_rules
            Type.BLOCKLIST -> R.string.firewall_rules
            Type.FIREWALL_WHITELIST -> R.string.whitelist
            Type.DECRYPTION_LIST -> R.string.decryption_rules
            else -> {
                assert(false)
                0
            }
        }
    }

    fun getHelpString(): Int {
        return when (mType) {
            Type.VISUALIZATION_MASK -> R.string.hidden_connections_help
            Type.MALWARE_WHITELIST -> R.string.malware_whitelist_help
            Type.BLOCKLIST -> 0
            Type.FIREWALL_WHITELIST -> R.string.firewall_whitelist_help
            Type.DECRYPTION_LIST -> R.string.decryption_rules_help
            else -> {
                assert(false)
                0
            }
        }
    }

    fun getSupportedRules(): Set<RuleType>? {
        return when (mType) {
            Type.VISUALIZATION_MASK -> ArraySet(
                listOf(
                    RuleType.APP,
                    RuleType.IP,
                    RuleType.HOST,
                    RuleType.COUNTRY,
                    RuleType.PROTOCOL
                )
            )

            Type.MALWARE_WHITELIST, Type.DECRYPTION_LIST, Type.BLOCKLIST -> ArraySet(
                listOf(
                    RuleType.APP, RuleType.IP, RuleType.HOST
                )
            )

            Type.FIREWALL_WHITELIST -> ArraySet(listOf(RuleType.APP))
        }
        assert(false)
        return null
    }

    fun reloadRules() {
        when (mType) {
            Type.MALWARE_WHITELIST -> CaptureService.getInstance().reloadMalwareWhitelist()
            Type.BLOCKLIST -> if (CaptureService.getInstance()
                    .isServiceActive()
            ) CaptureService.getInstance().requireInstance()
                .reloadBlocklist()

            Type.FIREWALL_WHITELIST -> if (CaptureService.getInstance()
                    .isServiceActive()
            ) CaptureService.getInstance().requireInstance()
                .reloadFirewallWhitelist()

            Type.DECRYPTION_LIST -> CaptureService.getInstance().reloadDecryptionList()
            Type.VISUALIZATION_MASK -> TODO()
        }
    }

//    // New Fragment - Disable
//    fun newFragment(): EditListFragment {
//        return EditListFragment.newInstance(mType)
//    }
}