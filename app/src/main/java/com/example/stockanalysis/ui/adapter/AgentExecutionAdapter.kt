package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.R
import com.example.stockanalysis.data.agent.AgentExecutionState
import com.example.stockanalysis.data.agent.AgentStatus
import com.example.stockanalysis.data.agent.AgentType
import com.example.stockanalysis.data.agent.Signal
import com.google.android.material.chip.Chip

/**
 * Agent 执行流程 Adapter
 */
class AgentExecutionAdapter : RecyclerView.Adapter<AgentExecutionAdapter.ViewHolder>() {

    private var states: List<AgentExecutionState> = emptyList()

    fun submitStates(newStates: List<AgentExecutionState>) {
        // 按优先级排序
        states = newStates.sortedBy { it.agentType.priority() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_execution, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(states[position])
    }

    override fun getItemCount(): Int = states.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        private val progressIndicator: ProgressBar = itemView.findViewById(R.id.progressIndicator)
        private val tvAgentName: TextView = itemView.findViewById(R.id.tvAgentName)
        private val tvAgentStatus: TextView = itemView.findViewById(R.id.tvAgentStatus)
        private val layoutResult: View = itemView.findViewById(R.id.layoutResult)
        private val chipSignal: Chip = itemView.findViewById(R.id.chipSignal)
        private val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)

        fun bind(state: AgentExecutionState) {
            val context = itemView.context

            // Agent 名称
            tvAgentName.text = state.agentType.displayName()

            // 状态显示
            when (state.status) {
                AgentStatus.PENDING -> {
                    progressIndicator.visibility = View.GONE
                    ivStatus.visibility = View.VISIBLE
                    ivStatus.setImageResource(R.drawable.ic_pending)
                    ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray_500))
                    tvAgentStatus.text = "等待执行"
                    layoutResult.visibility = View.GONE
                }
                AgentStatus.RUNNING -> {
                    progressIndicator.visibility = View.VISIBLE
                    ivStatus.visibility = View.GONE
                    tvAgentStatus.text = "分析中..."
                    layoutResult.visibility = View.GONE
                }
                AgentStatus.COMPLETED -> {
                    progressIndicator.visibility = View.GONE
                    ivStatus.visibility = View.VISIBLE
                    ivStatus.setImageResource(R.drawable.ic_check_circle)
                    ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.green_500))
                    tvAgentStatus.text = "分析完成"
                    showResult(state)
                }
                AgentStatus.FAILED -> {
                    progressIndicator.visibility = View.GONE
                    ivStatus.visibility = View.VISIBLE
                    ivStatus.setImageResource(R.drawable.ic_error)
                    ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.red_500))
                    tvAgentStatus.text = state.error ?: "执行失败"
                    layoutResult.visibility = View.GONE
                }
                AgentStatus.SKIPPED -> {
                    progressIndicator.visibility = View.GONE
                    ivStatus.visibility = View.VISIBLE
                    ivStatus.setImageResource(R.drawable.ic_skip)
                    ivStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
                    tvAgentStatus.text = "已跳过"
                    layoutResult.visibility = View.GONE
                }
            }
        }

        private fun showResult(state: AgentExecutionState) {
            layoutResult.visibility = View.VISIBLE

            val signal = state.signal ?: Signal.HOLD
            val confidence = state.confidence ?: 0f

            // 信号标签
            chipSignal.text = when (signal) {
                Signal.STRONG_BUY -> "强烈买入"
                Signal.BUY -> "买入"
                Signal.HOLD -> "观望"
                Signal.SELL -> "卖出"
                Signal.STRONG_SELL -> "强烈卖出"
            }

            // 信号颜色
            val colorRes = when (signal) {
                Signal.STRONG_BUY, Signal.BUY -> R.color.green_500
                Signal.HOLD -> R.color.orange_500
                Signal.SELL, Signal.STRONG_SELL -> R.color.red_500
            }
            chipSignal.setChipBackgroundColorResource(colorRes)

            // 置信度
            tvConfidence.text = "置信度: ${String.format("%.0f%%", confidence * 100)}"
        }
    }
}
