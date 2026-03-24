package com.example.stockanalysis.ui.import

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.stockanalysis.databinding.FragmentImportSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 导入设置Fragment
 */
@AndroidEntryPoint
class ImportSettingsFragment : Fragment() {

    private var _binding: FragmentImportSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels()

    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageImport(it) }
    }

    // CSV文件选择器
    private val csvPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleCsvImport(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        // 图片导入
        binding.btnImportImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // CSV导入
        binding.btnImportCsv.setOnClickListener {
            csvPickerLauncher.launch("*/*")
        }

        // 剪贴板导入
        binding.btnImportClipboard.setOnClickListener {
            showClipboardImportDialog()
        }

        // 导入历史
        binding.btnImportHistory.setOnClickListener {
            // TODO: 导航到导入历史页面
            Toast.makeText(requireContext(), "导入历史功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageImport(imageUri: Uri) {
        ImageImportPreviewDialog(imageUri) { result ->
            Toast.makeText(
                requireContext(),
                "成功导入${result.successCount}个股票",
                Toast.LENGTH_SHORT
            ).show()
        }.show(childFragmentManager, "ImageImportPreview")
    }

    private fun handleCsvImport(csvUri: Uri) {
        CSVImportPreviewDialog(csvUri) { result ->
            Toast.makeText(
                requireContext(),
                "成功导入${result.successCount}个股票",
                Toast.LENGTH_SHORT
            ).show()
        }.show(childFragmentManager, "CSVImportPreview")
    }

    private fun showClipboardImportDialog() {
        ClipboardImportDialog { result ->
            Toast.makeText(
                requireContext(),
                "成功导入${result.successCount}个股票",
                Toast.LENGTH_SHORT
            ).show()
        }.show(childFragmentManager, "ClipboardImport")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
