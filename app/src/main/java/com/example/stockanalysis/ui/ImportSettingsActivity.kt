package com.example.stockanalysis.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.stockanalysis.R
import com.example.stockanalysis.ui.import.ImportSettingsFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * 导入设置Activity
 */
@AndroidEntryPoint
class ImportSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import_settings)

        // 设置返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "智能导入设置"

        // 添加Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ImportSettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
