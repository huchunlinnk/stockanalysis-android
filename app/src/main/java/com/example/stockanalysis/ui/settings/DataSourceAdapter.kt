package com.example.stockanalysis.ui.settings

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.DataSourceConfig
import com.example.stockanalysis.databinding.ItemDataSourceBinding

/**
 * 数据源配置适配器
 *
 * 支持功能：
 * 1. 拖拽排序调整优先级
 * 2. 展开/折叠配置面板
 * 3. 启用/禁用数据源
 * 4. 测试连接
 */
class DataSourceAdapter(
    private val configs: MutableList<DataSourceConfig>,
    private val onTestClicked: (DataSourceConfig) -> Unit,
    private val onConfigChanged: (DataSourceConfig) -> Unit
) : RecyclerView.Adapter<DataSourceAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class ViewHolder(val binding: ItemDataSourceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(config: DataSourceConfig, position: Int) {
            binding.apply {
                // 基本信息
                tvPriority.text = config.priority.toString()
                tvName.text = config.displayName
                tvDescription.text = config.description
                tvStatus.text = "${config.getStatusIcon()} ${config.getStatusText()}"

                // 状态颜色
                tvStatus.setTextColor(
                    when {
                        !config.enabled -> Color.parseColor("#999999")
                        config.isHealthy -> Color.parseColor("#4CAF50")
                        else -> Color.parseColor("#F44336")
                    }
                )

                // 启用开关
                switchEnabled.isChecked = config.enabled
                switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                    config.enabled = isChecked
                    onConfigChanged(config)
                    notifyItemChanged(position)
                }

                // 配置区域显示/隐藏
                val isExpanded = expandedPositions.contains(position)
                layoutConfig.visibility = if (isExpanded) View.VISIBLE else View.GONE
                btnToggleConfig.text = if (isExpanded) "折叠配置" else "展开配置"

                // 展开/折叠按钮
                btnToggleConfig.setOnClickListener {
                    if (expandedPositions.contains(position)) {
                        expandedPositions.remove(position)
                    } else {
                        expandedPositions.add(position)
                    }
                    notifyItemChanged(position)
                }

                // API Key 配置
                if (config.requiresApiKey) {
                    layoutApiKey.visibility = View.VISIBLE
                    etApiKey.setText(config.apiKey)
                    etApiKey.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            config.apiKey = etApiKey.text.toString().trim()
                            onConfigChanged(config)
                        }
                    }
                } else {
                    layoutApiKey.visibility = View.GONE
                }

                // 额外配置字段
                layoutExtraFields.removeAllViews()
                if (config.configFields.isNotEmpty() && config.requiresConfig) {
                    layoutExtraFields.visibility = View.VISIBLE
                    // TODO: 动态生成配置字段
                } else {
                    layoutExtraFields.visibility = View.GONE
                }

                // 测试按钮
                btnTest.isEnabled = config.enabled && config.canTest()
                btnTest.setOnClickListener {
                    // 先保存当前配置
                    if (config.requiresApiKey) {
                        config.apiKey = etApiKey.text.toString().trim()
                    }
                    onTestClicked(config)
                }

                // 测试结果
                if (config.lastTestTime > 0) {
                    tvTestResult.text = config.lastTestResult
                    tvTestResult.setTextColor(
                        if (config.isHealthy) {
                            Color.parseColor("#4CAF50")
                        } else {
                            Color.parseColor("#F44336")
                        }
                    )
                } else {
                    tvTestResult.text = "未测试"
                    tvTestResult.setTextColor(Color.parseColor("#999999"))
                }

                // 禁用状态下灰色显示
                cardContainer.alpha = if (config.enabled) 1.0f else 0.6f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDataSourceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(configs[position], position)
    }

    override fun getItemCount(): Int = configs.size

    /**
     * 获取当前配置列表
     */
    fun getConfigs(): List<DataSourceConfig> {
        // 更新优先级（根据列表位置）
        configs.forEachIndexed { index, config ->
            config.priority = index + 1
        }
        return configs
    }

    /**
     * 更新测试结果
     */
    fun updateTestResult(configId: String, isSuccess: Boolean, message: String) {
        val index = configs.indexOfFirst { it.id == configId }
        if (index >= 0) {
            configs[index].apply {
                isHealthy = isSuccess
                lastTestTime = System.currentTimeMillis()
                lastTestResult = message
            }
            notifyItemChanged(index)
        }
    }

    /**
     * 移动 Item
     */
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val item = configs.removeAt(fromPosition)
        configs.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }

    /**
     * ItemTouchHelper 回调
     */
    class DragCallback(private val adapter: DataSourceAdapter) : ItemTouchHelper.Callback() {

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            return makeMovementFlags(dragFlags, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // 不支持滑动删除
        }

        override fun isLongPressDragEnabled(): Boolean = true
        override fun isItemViewSwipeEnabled(): Boolean = false
    }
}
