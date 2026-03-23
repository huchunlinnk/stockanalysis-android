package com.example.stockanalysis.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.stockanalysis.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * 数据源配置 Activity
 *
 * 承载 DataSourceConfigFragment
 */
@AndroidEntryPoint
class DataSourceConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_source_config)

        // 设置返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "数据源配置"

        // 加载 Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DataSourceConfigFragment.newInstance())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
