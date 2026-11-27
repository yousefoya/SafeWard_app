package com.example.safeward.ui.security

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.safeward.R
import com.example.safeward.model.SecurityLog

/**
 * Renders blocked/rejected security anomalies in the Security Audit Log feed
 * using the SafeWard Ultra-Light & Serene color palette.
 */
class SecurityLogAdapter : RecyclerView.Adapter<SecurityLogAdapter.ViewHolder>() {

    private val items = mutableListOf<SecurityLog>()

    fun submitList(logs: List<SecurityLog>) {
        items.clear()
        items.addAll(logs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_security_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val attackTypeBadge: TextView = itemView.findViewById(R.id.attackTypeBadge)
        private val actionBadge: TextView = itemView.findViewById(R.id.actionBadge)
        private val roomAndSourceText: TextView = itemView.findViewById(R.id.roomAndSourceText)
        private val reasonText: TextView = itemView.findViewById(R.id.reasonText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val logIdText: TextView = itemView.findViewById(R.id.logIdText)

        fun bind(log: SecurityLog) {
            val ctx = itemView.context

            attackTypeBadge.text = formatAttackLabel(log.attackType)
            val badgeColorRes = when (log.attackType) {
                "CROSS_LAYER_SPOOF" -> R.color.vitals_blush_petal
                "REPLAY_ATTACK" -> R.color.alert_warning_amber
                "UNAUTHORIZED_DEVICE" -> R.color.security_soft_lavender
                else -> R.color.surface_warm_linen
            }
            ViewCompat.setBackgroundTintList(
                attackTypeBadge,
                ColorStateList.valueOf(ContextCompat.getColor(ctx, badgeColorRes))
            )

            actionBadge.text = log.actionTaken
            roomAndSourceText.text = "Room ${log.roomId} • ${log.sourceId}"
            reasonText.text = log.reason
            timestampText.text = formatTimestamp(log.timestamp)
            logIdText.text = log.logId
        }

        private fun formatAttackLabel(type: String): String = when (type) {
            "CROSS_LAYER_SPOOF" -> "Cross-Layer Spoof"
            "REPLAY_ATTACK" -> "Replay Attack"
            "UNAUTHORIZED_DEVICE" -> "Unauthorized Device"
            else -> type.replace('_', ' ')
        }

        private fun formatTimestamp(raw: String): String =
            raw.replace('T', ' ').substringBefore('+')
    }
}
