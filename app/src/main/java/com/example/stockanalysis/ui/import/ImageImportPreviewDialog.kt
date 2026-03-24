package com.example.stockanalysis.ui.import

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.R
import com.example.stockanalysis.data.import.ImportResult
import com.example.stockanalysis.databinding.DialogImageImportPreviewBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 图片导入预览对话框
 */
@AndroidEntryPoint
class ImageImportPreviewDialog(
    private val imageUri: Uri,
    private val onImportComplete: (ImportResult) -> Unit
) : DialogFragment() {

    private var _binding: DialogImageImportPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels()
    private lateinit var adapter: ExtractedStockAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageImportPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadImage()
        startRecognition()
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
        binding.rvExtractedStocks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExtractedStocks.adapter = adapter
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

    private fun loadImage() {
        binding.ivPreview.setImageURI(imageUri)
    }

    private fun startRecognition() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = "正在识别图片中的股票..."

        viewModel.importFromImage(imageUri)
    }

    private fun observeViewModel() {
        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.visibility = View.GONE

            result.onSuccess { importResult ->
                if (importResult.importedStocks.isNotEmpty()) {
                    binding.rvExtractedStocks.visibility = View.VISIBLE
                    adapter.submitList(importResult.importedStocks.map {
                        com.example.stockanalysis.data.import.ExtractedStock(
                            code = it.symbol,
                            name = it.name,
                            confidence = "high"
                        )
                    })
                    binding.btnConfirm.isEnabled = true
                } else {
                    binding.tvStatus.visibility = View.VISIBLE
                    binding.tvStatus.text = "未识别到股票,请检查图片"
                }
            }.onFailure { error ->
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = "识别失败: ${error.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
