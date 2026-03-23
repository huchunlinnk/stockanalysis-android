package com.example.stockanalysis.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.stockanalysis.R
import com.example.stockanalysis.data.local.PreferencesManager
import com.example.stockanalysis.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否首次启动
        if (preferencesManager.isFirstLaunch()) {
            // 跳转到引导页面
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // 检查是否已接受免责声明
        if (!preferencesManager.isDisclaimerAccepted()) {
            showDisclaimerDialog()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置 ActionBar
        setSupportActionBar(binding.toolbar)

        setupNavigation()
    }

    /**
     * 显示免责声明对话框
     * 用户必须同意才能继续使用应用
     */
    private fun showDisclaimerDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.first_launch_disclaimer_title)
            .setMessage(R.string.first_launch_disclaimer_message)
            .setCancelable(false)
            .setPositiveButton(R.string.agree_and_continue) { _, _ ->
                // 用户同意免责声明
                preferencesManager.setDisclaimerAccepted(true)
                // 初始化主界面
                initMainUi()
            }
            .setNegativeButton(R.string.view_full_terms) { dialog, _ ->
                // 打开关于页面查看完整条款
                dialog.dismiss()
                startActivity(Intent(this, AboutActivity::class.java))
                // 重新显示免责声明对话框
                showDisclaimerDialog()
            }
            .setNeutralButton(R.string.cancel) { _, _ ->
                // 用户不同意，显示提示并退出
                Toast.makeText(this, R.string.must_agree_to_continue, Toast.LENGTH_LONG).show()
                finish()
            }
            .show()
    }

    /**
     * 初始化主界面
     */
    private fun initMainUi() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置 ActionBar
        setSupportActionBar(binding.toolbar)

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // 打开设置页面
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
