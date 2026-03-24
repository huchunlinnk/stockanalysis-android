package com.example.stockanalysis.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.stockanalysis.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 导出选项对话框
 * 让用户选择导出格式：Markdown、图片、纯文本
 */
class ExportDialog : DialogFragment() {

    private var listener: ExportListener? = null

    interface ExportListener {
        /**
         * 导出为Markdown格式
         */
        fun onExportMarkdown()

        /**
         * 导出为图片
         */
        fun onExportImage()

        /**
         * 导出为纯文本
         */
        fun onExportText()

        /**
         * 分享到其他应用
         */
        fun onShareToOthers()

        /**
         * 保存到本地
         */
        fun onSaveToLocal()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = when {
            parentFragment is ExportListener -> parentFragment as ExportListener
            context is ExportListener -> context
            else -> throw IllegalStateException("Activity or parent fragment must implement ExportListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val options = arrayOf(
            "📄 导出为Markdown",
            "🖼️ 导出为图片",
            "📝 导出为纯文本",
            "📤 分享到其他应用",
            "💾 保存到本地"
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择导出方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> listener?.onExportMarkdown()
                    1 -> listener?.onExportImage()
                    2 -> listener?.onExportText()
                    3 -> listener?.onShareToOthers()
                    4 -> listener?.onSaveToLocal()
                }
            }
            .setNegativeButton("取消", null)
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        const val TAG = "ExportDialog"

        fun newInstance(): ExportDialog {
            return ExportDialog()
        }
    }
}
