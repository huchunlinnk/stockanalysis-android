package com.example.stockanalysis.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stockanalysis.BuildConfig
import com.example.stockanalysis.R
import com.example.stockanalysis.databinding.ActivityAboutBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * 关于页面
 *
 * 功能：
 * 1. 显示应用名称和版本信息
 * 2. 声明基于开源项目 daily_stock_analysis 二次开发
 * 3. 显示完整的免责声明
 * 4. 显示开源协议说明（MIT）
 * 5. 提供查看完整免责声明的入口
 */
@AndroidEntryPoint
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        loadVersionInfo()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        // 开源项目链接
        binding.btnOpenSourceLink.setOnClickListener {
            openUrl("https://github.com/ZhuLinsen/daily_stock_analysis")
        }

        // 查看完整免责声明
        binding.btnViewDisclaimer.setOnClickListener {
            showFullDisclaimer()
        }

        // 隐私政策
        binding.btnPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }

        // 服务条款
        binding.btnTermsOfService.setOnClickListener {
            showTermsOfService()
        }
    }

    private fun loadVersionInfo() {
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        binding.tvVersion.text = "v$versionName ($versionCode)"
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFullDisclaimer() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.disclaimer_title)
            .setMessage(R.string.full_disclaimer)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    private fun showPrivacyPolicy() {
        val privacyText = """
隐私政策

1. 数据收集
本应用不会主动收集用户的个人信息。所有数据均存储在本地设备上。

2. 数据使用
• 股票数据：通过第三方公开接口获取，仅用于分析和展示
• API密钥：用户配置的LLM API密钥仅保存在本地加密存储中
• 使用记录：分析历史仅保存在本地数据库中

3. 数据安全
• 敏感信息（API密钥等）使用Android Keystore加密存储
• 应用数据不会上传至任何服务器
• 用户可以清除所有本地数据

4. 第三方服务
本应用可能使用以下第三方服务：
• Tushare：获取股票财务数据
• 各LLM提供商：进行智能分析

5. 用户权利
用户可以随时：
• 清除所有本地数据
• 撤销已授权的权限
• 卸载应用删除所有数据

本政策最后更新日期：2026年3月
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.privacy_policy)
            .setMessage(privacyText)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }

    private fun showTermsOfService() {
        val termsText = """
服务条款

1. 接受条款
使用本应用即表示您同意本条款。如果您不同意，请勿使用本应用。

2. 服务说明
本应用提供股票分析工具，所有分析结果仅供参考，不构成投资建议。

3. 用户责任
• 用户应自行承担使用本应用的风险
• 用户应确保合法合规使用本应用
• 用户不得将本应用用于非法目的

4. 免责声明
• 本应用不对分析结果的准确性和完整性作任何保证
• 本应用不对因使用本应用而产生的任何损失承担责任
• 投资有风险，入市需谨慎

5. 知识产权
本应用基于 MIT 协议开源，原项目地址：
https://github.com/ZhuLinsen/daily_stock_analysis

6. 条款修改
本应用保留随时修改本条款的权利，修改后的条款将在应用内公布。

7. 适用法律
本条款受中华人民共和国法律管辖。
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.terms_of_service)
            .setMessage(termsText)
            .setPositiveButton(R.string.confirm, null)
            .show()
    }
}
