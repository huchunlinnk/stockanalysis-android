package com.example.stockanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.stockanalysis.R
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.databinding.ActivityOnboardingBinding
import com.example.stockanalysis.databinding.OnboardingPageBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 首次使用引导页面
 *
 * 功能：
 * 1. 介绍应用核心功能
 * 2. 引导用户配置 API Key 和 Tushare Token
 * 3. 说明配置后的功能增强
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val pages = listOf(
        OnboardingPage(
            title = "欢迎使用股票分析应用",
            description = "专业的股票技术分析工具\n" +
                    "支持 A 股、港股、美股\n" +
                    "提供智能分析和投资建议",
            icon = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "AI 智能分析",
            description = "配置 LLM API Key 后\n" +
                    "可使用 AI 进行智能分析和舆情分析\n" +
                    "支持多种模型（OpenAI、Deepseek 等）",
            icon = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "真实财务数据",
            description = "配置 Tushare Token 后\n" +
                    "可获取真实的财务数据和基本面指标\n" +
                    "提升分析的准确性和专业性",
            icon = R.drawable.ic_launcher_foreground
        ),
        OnboardingPage(
            title = "开始使用",
            description = "点击\"立即配置\"进入设置页面\n" +
                    "或选择\"稍后配置\"直接开始使用\n" +
                    "（未配置时将使用模拟数据）",
            icon = R.drawable.ic_launcher_foreground,
            isLast = true
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(pages) { position, isSkip ->
            if (isSkip || position == pages.size - 1) {
                finishOnboarding(isSkip)
            } else {
                binding.viewPager.currentItem = position + 1
            }
        }

        binding.viewPager.adapter = adapter
    }

    private fun finishOnboarding(isSkip: Boolean) {
        // 标记首次启动完成
        preferencesManager.setFirstLaunchCompleted()

        if (!isSkip && !preferencesManager.isApiConfigured()) {
            // 跳转到设置页面
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 跳转到主页面
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /**
     * 引导页数据
     */
    data class OnboardingPage(
        val title: String,
        val description: String,
        val icon: Int,
        val isLast: Boolean = false
    )

    /**
     * ViewPager 适配器
     */
    class OnboardingAdapter(
        private val pages: List<OnboardingPage>,
        private val onAction: (position: Int, isSkip: Boolean) -> Unit
    ) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        class ViewHolder(val binding: OnboardingPageBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = OnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]

            holder.binding.apply {
                tvTitle.text = page.title
                tvDescription.text = page.description
                ivIcon.setImageResource(page.icon)

                if (page.isLast) {
                    btnSkip.text = "稍后配置"
                    btnNext.text = "立即配置"
                } else {
                    btnSkip.text = "跳过"
                    btnNext.text = "下一步"
                }

                btnSkip.setOnClickListener {
                    onAction(position, true)
                }

                btnNext.setOnClickListener {
                    onAction(position, false)
                }
            }
        }

        override fun getItemCount(): Int = pages.size
    }
}
