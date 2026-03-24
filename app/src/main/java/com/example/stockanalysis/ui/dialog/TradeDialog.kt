package com.example.stockanalysis.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.example.stockanalysis.data.portfolio.PortfolioHolding
import com.example.stockanalysis.databinding.DialogTradeBinding

class TradeDialog : DialogFragment() {

    private var _binding: DialogTradeBinding? = null
    private val binding get() = _binding!!

    private var onTradeListener: ((
        type: String,
        quantity: Int,
        price: Double,
        commission: Double
    ) -> Unit)? = null

    private lateinit var holding: PortfolioHolding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 从arguments获取holding信息
        arguments?.let {
            val symbol = it.getString("symbol") ?: return
            val name = it.getString("name") ?: return
            val availableQty = it.getInt("availableQuantity")
            val currentPrice = it.getDouble("currentPrice")

            setupViews(symbol, name, availableQty, currentPrice)
        }

        setupListeners()
    }

    private fun setupViews(symbol: String, name: String, availableQty: Int, currentPrice: Double) {
        binding.apply {
            tvStockInfo.text = "$name ($symbol)"
            tvAvailableQuantity.text = "可用: $availableQty 股"
            etPrice.setText(currentPrice.toString())
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSell.setOnClickListener {
            if (validateInput()) {
                val quantity = binding.etQuantity.text.toString().toInt()
                val price = binding.etPrice.text.toString().toDouble()
                val commission = binding.etCommission.text.toString().toDoubleOrNull() ?: 0.0

                onTradeListener?.invoke("SELL", quantity, price, commission)
                dismiss()
            }
        }
    }

    private fun validateInput(): Boolean {
        binding.apply {
            val quantityStr = etQuantity.text.toString()
            if (quantityStr.isEmpty()) {
                etQuantity.error = "请输入卖出数量"
                return false
            }
            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                etQuantity.error = "数量必须大于0"
                return false
            }

            val availableQty = arguments?.getInt("availableQuantity") ?: 0
            if (quantity > availableQty) {
                etQuantity.error = "卖出数量不能超过可用数量"
                return false
            }

            val priceStr = etPrice.text.toString()
            if (priceStr.isEmpty()) {
                etPrice.error = "请输入卖出价格"
                return false
            }
            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0) {
                etPrice.error = "价格必须大于0"
                return false
            }

            return true
        }
    }

    fun setOnTradeListener(listener: (String, Int, Double, Double) -> Unit) {
        onTradeListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(holding: PortfolioHolding): TradeDialog {
            return TradeDialog().apply {
                arguments = bundleOf(
                    "symbol" to holding.stockSymbol,
                    "name" to holding.stockName,
                    "availableQuantity" to holding.availableQuantity,
                    "currentPrice" to holding.currentPrice
                )
            }
        }
    }
}
