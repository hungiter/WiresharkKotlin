package com.example.ptnet.models

import android.view.LayoutInflater
import com.example.ptnet.PCAPdroid
import com.example.ptnet.models.ConnectionDescriptor.Status
import com.example.ptnet.models.ConnectionDescriptor.DecryptionStatus
import com.example.ptnet.models.ConnectionDescriptor.FilteringStatus
import com.example.ptnet.services.CaptureService
import java.io.Serializable
import java.util.Locale
import com.example.ptnet.R


class FilterDescriptor() : Serializable {
    var status: Status? = null
    var showMasked = false
    var onlyBlacklisted = false
    var onlyCleartext = false
    var filteringStatus: FilteringStatus? = null
    var decStatus: DecryptionStatus? = null
    var iface: String? = null
    var uid = -2 // this is persistent and used internally (AppDetailsActivity)


    init {
        clear()
        assert(!isSet())
    }

    fun isSet(): Boolean {
        return (status !== Status.STATUS_INVALID || decStatus != DecryptionStatus.INVALID || filteringStatus != FilteringStatus.INVALID || iface != null
                || onlyBlacklisted
                || onlyCleartext || uid != -2) || !showMasked && !PCAPdroid().getInstance()
            .getVisualizationMask().isEmpty()
    }

    fun matches(conn: ConnectionDescriptor): Boolean {
        return ((showMasked || !PCAPdroid().getInstance().getVisualizationMask().matches(conn))
                && (!onlyBlacklisted || conn.isBlacklisted())
                && (!onlyCleartext || conn.isCleartext())
                && (status == Status.STATUS_INVALID || conn.getStatus() == status)
                && (decStatus == DecryptionStatus.INVALID || conn.getDecryptionStatus() == decStatus)
                && (filteringStatus == FilteringStatus.INVALID || filteringStatus == FilteringStatus.BLOCKED == conn.isBlocked)
                && (iface == null || CaptureService().getInterfaceName(conn.ifidx) == iface)
                && (uid == -2 || uid == conn.uid))
    }

//    // Filter Display - Disable
//    private fun addChip(inflater: LayoutInflater, group: ChipGroup, id: Int, text: String) {
//        val chip: Chip = inflater.inflate(R.layout.active_filter_chip, group, false) as Chip
//        chip.setId(id)
//        chip.setText(text.lowercase(Locale.getDefault()))
//        group.addView(chip)
//    }

//    fun toChips(inflater: LayoutInflater, group: ChipGroup) {
//        val ctx = inflater.context
//        if (!showMasked) addChip(
//            inflater,
//            group,
//            R.id.not_hidden,
//            ctx.getString(R.string.not_hidden_filter)
//        )
//        if (onlyBlacklisted) addChip(
//            inflater,
//            group,
//            R.id.blacklisted,
//            ctx.getString(R.string.malicious_connection_filter)
//        )
//        if (onlyCleartext) addChip(
//            inflater,
//            group,
//            R.id.only_cleartext,
//            ctx.getString(R.string.cleartext_connection)
//        )
//        if (status !== Status.STATUS_INVALID) {
//            val label = String.format(
//                ctx.getString(R.string.status_filter),
//                ConnectionDescriptor.getStatusLabel(status, ctx)
//            )
//            addChip(inflater, group, R.id.status_ind, label)
//        }
//        if (decStatus != DecryptionStatus.INVALID) {
//            val label = String.format(
//                ctx.getString(R.string.decryption_filter),
//                ConnectionDescriptor.getDecryptionStatusLabel(decStatus, ctx)
//            )
//            addChip(inflater, group, R.id.decryption_status, label)
//        }
//        if (filteringStatus != FilteringStatus.INVALID) {
//            val label = ctx.getString(
//                R.string.firewall_filter,
//                ctx.getString(if (filteringStatus == FilteringStatus.BLOCKED) R.string.blocked_connection_filter else R.string.allowed_connection_filter)
//            )
//            addChip(inflater, group, R.id.firewall, label)
//        }
//        if (iface != null) addChip(
//            inflater,
//            group,
//            R.id.capture_interface,
//            String.format(ctx.getString(R.string.interface_filter), iface)
//        )
//    }

//    // Filter By Resources ID - Disable
//    fun clear(filterId: Int) {
//        if (filterId == R.id.not_hidden) showMasked =
//            true else if (filterId == R.id.blacklisted) onlyBlacklisted =
//            false else if (filterId == R.id.only_cleartext) onlyCleartext =
//            false else if (filterId == R.id.status_ind) status =
//            Status.STATUS_INVALID else if (filterId == R.id.decryption_status) decStatus =
//            DecryptionStatus.INVALID else if (filterId == R.id.firewall) filteringStatus =
//            FilteringStatus.INVALID else if (filterId == R.id.capture_interface) iface = null
//    }

    fun clear() {
        showMasked = true
        onlyBlacklisted = false
        onlyCleartext = false
        status = Status.STATUS_INVALID
        decStatus = DecryptionStatus.INVALID
        filteringStatus = FilteringStatus.INVALID
        iface = null
    }
}