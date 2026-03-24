package com.example.stockanalysis.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.stockanalysis.R
import com.example.stockanalysis.databinding.DialogAddTransactionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddTransactionDialog : DialogFragment() {

    private var _binding: DialogAddTransactionBinding? = null
    private val binding get() = _binding!!

    private var onTransactionListener: ((
        type: String,
        symbol: String,
        name: String,
        quantity: Int,
        price: Double,
        commission: Double
    ) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // 交易类型下拉框
        val types = arrayOf("买入", "卖出")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            if (validateInput()) {
                val type = if (binding.spinnerType.selectedItemPosition == 0) "BUY" else "SELL"
                val symbol = binding.etSymbol.text.toString().trim().uppercase()
                val name = binding.etName.text.toString().trim()
                val quantity = binding.etQuantity.text.toString().toInt()
                val price = binding.etPrice.text.toString().toDouble()
                val commission = binding.etCommission.text.toString().toDoubleOrNull() ?: 0.0

                onTransactionListener?.invoke(type, symbol, name, quantity, price, commission)
                dismiss()
            }
        }
    }

    private fun validateInput(): Boolean {
        binding.apply {
            val symbol = etSymbol.text.toString().trim()
            if (symbol.isEmpty()) {
                etSymbol.error = "请输入股票代码"
                return false
            }

            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "请输入股票名称"
                return false
            }

            val quantityStr = etQuantity.text.toString()
            if (quantityStr.isEmpty()) {
                etQuantity.error = "请输入数量"
                return false
            }
            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                etQuantity.error = "数量必须大于0"
                return false
            }

            val priceStr = etPrice.text.toString()
            if (priceStr.isEmpty()) {
                etPrice.error = "请输入价格"
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

    fun setOnTransactionListener(listener: (String, String, String, Int, Double, Double) -> Unit) {
        onTransactionListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddTransactionDialog()
    }
}
