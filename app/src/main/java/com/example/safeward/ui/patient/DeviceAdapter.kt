package com.example.safeward.ui.patient

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.safeward.R
import com.example.safeward.model.Device

/** RecyclerView adapter for ESP32 CSI and medical sensor nodes. */
class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val items = mutableListOf<Device>()

    fun submitList(newItems: List<Device>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_node, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(items[position], onDeviceClick)
    }

    override fun getItemCount(): Int = items.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceIdText: TextView = itemView.findViewById(R.id.deviceIdText)
        private val deviceTypeRoomText: TextView = itemView.findViewById(R.id.deviceTypeRoomText)
        private val deviceStatusBadge: TextView = itemView.findViewById(R.id.deviceStatusBadge)
        private val rssiText: TextView = itemView.findViewById(R.id.rssiText)
        private val tlsBadge: TextView = itemView.findViewById(R.id.tlsBadge)
        private val lastSeenText: TextView = itemView.findViewById(R.id.lastSeenText)

        fun bind(device: Device, onDeviceClick: (Device) -> Unit) {
            val ctx = itemView.context
            deviceIdText.text = device.deviceId
            deviceTypeRoomText.text = "${device.type} • Room ${device.roomId}"

            val isOnline = device.status.equals("ONLINE", ignoreCase = true)
            deviceStatusBadge.text = device.status
            val statusBg = if (isOnline) R.color.status_safe_mint else R.color.alert_critical_rose
            val statusFg = if (isOnline) R.color.text_soft_charcoal else R.color.white
            deviceStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, statusBg))
            deviceStatusBadge.setTextColor(ContextCompat.getColor(ctx, statusFg))

            val signalQuality = when {
                device.rssiDbm >= -55 -> "Strong"
                device.rssiDbm >= -70 -> "Good"
                else -> "Weak"
            }
            rssiText.text = "Signal: ${device.rssiDbm} dBm ($signalQuality)"

            tlsBadge.text = if (device.tlsVerified) "TLS OK" else "TLS FAIL"
            val tlsBg = if (device.tlsVerified) R.color.csi_soft_sky_mist else R.color.vitals_blush_petal
            tlsBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, tlsBg))

            lastSeenText.text = device.lastSeen.substringAfter('T').take(5).ifEmpty { "--:--" }
            itemView.setOnClickListener { onDeviceClick(device) }
        }
    }
}
