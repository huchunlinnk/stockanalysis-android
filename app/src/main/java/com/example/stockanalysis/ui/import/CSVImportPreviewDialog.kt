package com.example.stockanalysis.ui.import

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.data.import.ImportResult
import com.example.stockanalysis.databinding.DialogCsvImportPreviewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * CSV导入预览对话框
 */
@AndroidEntryPoint
class CSVImportPreviewDialog(
    private val csvUri: Uri,
    private val onImportComplete: (ImportResult) -> Unit
) : DialogFragment() {

    private var _binding: DialogCsvImportPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels()
    private lateinit var adapter: ExtractedStockAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCsvImportPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadCsvFile()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupRecyclerView() {
        adapter = ExtractedStockAdapter()
        binding.rvCsvData.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCsvData.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            viewModel.importResult.value?.getOrNull()?.let { result ->
                onImportComplete(result)
                dismiss()
            }
        }
    }

    private fun loadCsvFile() {
        binding.tvFileName.text = "文件: ${csvUri.lastPathSegment}"
        viewModel.importFromCsv(csvUri)
    }

    private fun observeViewModel() {
        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { importResult ->
                if (importResult.importedStocks.isNotEmpty()) {
                    adapter.submitList(importResult.importedStocks.map {
                        com.example.stockanalysis.data.import.ExtractedStock(
                            code = it.symbol,
                            name = it.name,
                            confidence = "high"
                        )
                    })
                    binding.tvImportStats.text = "共${importResult.totalCount}条数据, " +
                            "成功${importResult.successCount}条, " +
                            "失败${importResult.failCount}条"
                } else {
                    binding.tvImportStats.text = "未找到有效数据"
                }
            }.onFailure { error ->
                binding.tvImportStats.text = "解析失败: ${error.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
