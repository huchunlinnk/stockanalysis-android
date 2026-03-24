package com.example.stockanalysis.ui.import

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.example.stockanalysis.data.import.ImportResult
import com.example.stockanalysis.databinding.DialogClipboardImportBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 剪贴板导入对话框
 */
@AndroidEntryPoint
class ClipboardImportDialog(
    private val onImportComplete: (ImportResult) -> Unit
) : DialogFragment() {

    private var _binding: DialogClipboardImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogClipboardImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupButtons() {
        // 粘贴按钮
        binding.btnPaste.setOnClickListener {
            pasteFromClipboard()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 确认导入按钮
        binding.btnConfirm.setOnClickListener {
            val text = binding.etClipboardContent.text?.toString() ?: ""
            if (text.isNotBlank()) {
                viewModel.importFromClipboard(text)
            }
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            binding.etClipboardContent.setText(text)

            // 自动解析
            if (text.isNotBlank()) {
                binding.btnConfirm.isEnabled = true
            }
        }
    }

    private fun observeViewModel() {
        viewModel.importResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { importResult ->
                binding.tvParseResult.visibility = View.VISIBLE
                binding.tvParseResult.text = "解析成功: 共${importResult.totalCount}条, " +
                        "成功${importResult.successCount}条, " +
                        "失败${importResult.failCount}条"

                if (importResult.successCount > 0) {
                    onImportComplete(importResult)
                    dismiss()
                }
            }.onFailure { error ->
                binding.tvParseResult.visibility = View.VISIBLE
                binding.tvParseResult.text = "解析失败: ${error.message}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
