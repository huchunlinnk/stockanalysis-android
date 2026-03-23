package com.example.stockanalysis.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stockanalysis.databinding.ItemStockBinding

class SearchResultAdapter(
    private val onItemClick: (Pair<String, String>) -> Unit
) : ListAdapter<Pair<String, String>, SearchResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<String, String>) {
            binding.tvStockName.text = item.second
            binding.tvPrice.text = item.first
            binding.tvChange.text = ""
            
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Pair<String, String>>() {
        override fun areItemsTheSame(oldItem: Pair<String, String>, newItem: Pair<String, String>): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: Pair<String, String>, newItem: Pair<String, String>): Boolean {
            return oldItem == newItem
        }
    }
}
