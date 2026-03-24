package com.example.stockanalysis.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.stockanalysis.R

/**
 * 技术分析Fragment
 * TODO: 实现技术指标展示
 */
class TechnicalAnalysisFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = TextView(requireContext()).apply {
            text = "技术分析功能开发中..."
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
        return view
    }

    companion object {
        fun newInstance() = TechnicalAnalysisFragment()
    }
}
