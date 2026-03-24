package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.R
import com.example.stockanalysis.data.agent.AgentOpinion
import com.example.stockanalysis.data.agent.AgentType
import com.example.stockanalysis.data.agent.Signal
import com.google.android.material.chip.Chip

/**
 * Agent 意见 Adapter
 */
class AgentOpinionAdapter : RecyclerView.Adapter<AgentOpinionAdapter.ViewHolder>() {

    private var opinions: List<AgentOpinion> = emptyList()

    fun submitOpinions(newOpinions: List<AgentOpinion>) {
        // 过滤掉决策 Agent，只显示分析 Agent
        opinions = newOpinions.filter { it.agentType != AgentType.DECISION }
            .sortedBy { it.agentType.priority() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_opinion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(opinions[position])
    }

    override fun getItemCount(): Int = opinions.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAgentName: TextView = itemView.findViewById(R.id.tvAgentName)
        private val chipSignal: Chip = itemView.findViewById(R.id.chipSignal)
        private val progressConfidence: ProgressBar = itemView.findViewById(R.id.progressConfidence)
        private val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        private val tvReasoning: TextView = itemView.findViewById(R.id.tvReasoning)
        private val layoutKeyLevels: View = itemView.findViewById(R.id.layoutKeyLevels)
        private val tvSupport: TextView = itemView.findViewById(R.id.tvSupport)
        private val tvResistance: TextView = itemView.findViewById(R.id.tvResistance)

        fun bind(opinion: AgentOpinion) {
            val context = itemView.context

            // Agent 名称
            tvAgentName.text = opinion.agentType.displayName()

            // 信号
            val signalText = when (opinion.signal) {
                Signal.STRONG_BUY -> "强烈买入"
                Signal.BUY -> "买入"
                Signal.HOLD -> "观望"
                Signal.SELL -> "卖出"
                Signal.STRONG_SELL -> "强烈卖出"
            }
            chipSignal.text = signalText

            // 信号颜色
            val (bgColor, textColor) = when (opinion.signal) {
                Signal.STRONG_BUY, Signal.BUY -> 
                    Pair(R.color.green_100, R.color.green_800)
                Signal.HOLD -> 
                    Pair(R.color.orange_100, R.color.orange_800)
                Signal.SELL, Signal.STRONG_SELL -> 
                    Pair(R.color.red_100, R.color.red_800)
            }
            chipSignal.setChipBackgroundColorResource(bgColor)
            chipSignal.setTextColor(ContextCompat.getColor(context, textColor))

            // 置信度
            val confidencePercent = (opinion.confidence * 100).toInt()
            progressConfidence.progress = confidencePercent
            tvConfidence.text = "${confidencePercent}%"

            // 置信度颜色
            val progressColor = when {
                confidencePercent >= 80 -> R.color.green_500
                confidencePercent >= 50 -> R.color.orange_500
                else -> R.color.red_500
            }
            progressConfidence.progressTintList = ContextCompat.getColorStateList(context, progressColor)

            // 分析理由
            tvReasoning.text = opinion.reasoning

            // 关键价位
            opinion.keyLevels?.let { levels ->
                val hasLevels = levels.support != null || levels.resistance != null
                layoutKeyLevels.visibility = if (hasLevels) View.VISIBLE else View.GONE

                levels.support?.let {
                    tvSupport.text = "支撑: ${String.format("%.2f", it)}"
                } ?: run {
                    tvSupport.visibility = View.GONE
                }

                levels.resistance?.let {
                    tvResistance.text = "阻力: ${String.format("%.2f", it)}"
                } ?: run {
                    tvResistance.visibility = View.GONE
                }
            } ?: run {
                layoutKeyLevels.visibility = View.GONE
            }
        }
    }
}
