package com.example.stockanalysis.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.stockanalysis.ui.fragment.FundamentalDataFragment
import com.example.stockanalysis.ui.fragment.TechnicalAnalysisFragment
import com.example.stockanalysis.ui.fragment.NewsFragment

/**
 * 股票详情页面适配器
 * 用于管理不同的数据展示Tab
 */
class StockDetailPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val TAB_FUNDAMENTAL = 0
        const val TAB_TECHNICAL = 1
        const val TAB_NEWS = 2
        const val TAB_COUNT = 3

        val TAB_TITLES = arrayOf(
            "基本面",
            "技术面",
            "新闻资讯"
        )
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_FUNDAMENTAL -> FundamentalDataFragment.newInstance()
            TAB_TECHNICAL -> TechnicalAnalysisFragment.newInstance()
            TAB_NEWS -> NewsFragment.newInstance()
            else -> FundamentalDataFragment.newInstance()
        }
    }
}
