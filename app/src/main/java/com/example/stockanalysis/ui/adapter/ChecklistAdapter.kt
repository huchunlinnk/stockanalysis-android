package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.data.model.CheckItem
import com.example.stockanalysis.data.model.CheckStatus
import com.example.stockanalysis.databinding.ItemChecklistBinding

/**
 * 检查清单适配器
 */
class ChecklistAdapter(
    private val items: List<CheckItem>
) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChecklistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemChecklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CheckItem) {
            binding.tvItem.text = item.item
            
            val (statusIcon, statusColor) = when (item.status) {
                CheckStatus.PASSED -> 
                    android.R.drawable.checkbox_on_background to android.R.color.holo_green_dark
                CheckStatus.WARNING -> 
                    android.R.drawable.ic_dialog_alert to android.R.color.holo_orange_dark
                CheckStatus.FAILED -> 
                    android.R.drawable.ic_delete to android.R.color.holo_red_dark
            }
            
            binding.ivStatus.setImageResource(statusIcon)
            binding.ivStatus.setColorFilter(binding.root.context.getColor(statusColor))
        }
    }
}
