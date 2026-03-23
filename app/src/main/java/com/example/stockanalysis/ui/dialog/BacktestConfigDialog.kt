package com.example.stockanalysis.ui.dialog

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.stockanalysis.databinding.DialogBacktestConfigBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BacktestConfigDialog : DialogFragment() {

    private var _binding: DialogBacktestConfigBinding? = null
    private val binding get() = _binding!!

    private var onConfigListener: ((
        symbol: String,
        strategyId: String?,
        startDate: Date?,
        endDate: Date?
    ) -> Unit)? = null

    private var startDate: Date? = null
    private var endDate: Date? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBacktestConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // 设置默认日期范围（最近30天）
        val calendar = Calendar.getInstance()
        endDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDate = calendar.time

        updateDateDisplay()
    }

    private fun setupListeners() {
        binding.btnStartDate.setOnClickListener {
            showDatePicker(startDate ?: Date()) { date ->
                startDate = date
                updateDateDisplay()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(endDate ?: Date()) { date ->
                endDate = date
                updateDateDisplay()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnStart.setOnClickListener {
            if (validateInput()) {
                val symbol = binding.etSymbol.text.toString().trim().uppercase()
                val strategyId = null  // TODO: Add strategy selection
                onConfigListener?.invoke(symbol, strategyId, startDate, endDate)
                dismiss()
            }
        }
    }

    private fun updateDateDisplay() {
        startDate?.let {
            binding.btnStartDate.text = dateFormat.format(it)
        }
        endDate?.let {
            binding.btnEndDate.text = dateFormat.format(it)
        }
    }

    private fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = initialDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun validateInput(): Boolean {
        binding.apply {
            val symbol = etSymbol.text.toString().trim()
            if (symbol.isEmpty()) {
                etSymbol.error = "请输入股票代码"
                return false
            }

            if (startDate == null) {
                // Show error
                return false
            }

            if (endDate == null) {
                // Show error
                return false
            }

            if (startDate!!.after(endDate)) {
                // Show error
                return false
            }

            return true
        }
    }

    fun setOnConfigListener(listener: (String, String?, Date?, Date?) -> Unit) {
        onConfigListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BacktestConfigDialog()
    }
}
