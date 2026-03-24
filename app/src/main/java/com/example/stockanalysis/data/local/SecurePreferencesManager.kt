package com.example.stockanalysis.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全配置管理器
 *
 * 使用 EncryptedSharedPreferences 加密存储敏感数据：
 * - LLM API Key
 * - Tushare Token
 * - 其他需要加密的配置
 *
 * 基于 AndroidKeyStore 的 AES256-GCM 加密
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecurePreferencesManager"
        private const val SECURE_PREFS_NAME = "secure_prefs"

        // 加密存储的 Key
        private const val KEY_LLM_API_KEY = "llm_api_key_encrypted"
        private const val KEY_TUSHARE_TOKEN = "tushare_token_encrypted"
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to normal", e)
            // 降级到普通 SharedPreferences（仅用于测试或极端情况）
            context.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * 保存 LLM API Key（加密）
     */
    fun setLLMApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_LLM_API_KEY, apiKey).apply()
        Log.d(TAG, "LLM API Key saved (encrypted)")
    }

    /**
     * 获取 LLM API Key（解密）
     */
    fun getLLMApiKey(): String {
        return securePrefs.getString(KEY_LLM_API_KEY, "") ?: ""
    }

    /**
     * 保存 Tushare Token（加密）
     */
    fun setTushareToken(token: String) {
        securePrefs.edit().putString(KEY_TUSHARE_TOKEN, token).apply()
        Log.d(TAG, "Tushare Token saved (encrypted)")
    }

    /**
     * 获取 Tushare Token（解密）
     */
    fun getTushareToken(): String {
        return securePrefs.getString(KEY_TUSHARE_TOKEN, "") ?: ""
    }

    /**
     * 清除所有加密数据
     */
    fun clearAll() {
        securePrefs.edit().clear().apply()
        Log.d(TAG, "All secure data cleared")
    }

    /**
     * 检查是否有 LLM API Key
     */
    fun hasLLMApiKey(): Boolean {
        return getLLMApiKey().isNotEmpty()
    }

    /**
     * 检查是否有 Tushare Token
     */
    fun hasTushareToken(): Boolean {
        return getTushareToken().isNotEmpty()
    }

    /**
     * 从旧的明文存储迁移到加密存储
     *
     * @param oldApiKey 旧的 API Key
     * @param oldToken 旧的 Tushare Token
     */
    fun migrateFromPlainText(oldApiKey: String, oldToken: String) {
        if (oldApiKey.isNotEmpty() && !hasLLMApiKey()) {
            setLLMApiKey(oldApiKey)
            Log.d(TAG, "Migrated LLM API Key from plain text")
        }

        if (oldToken.isNotEmpty() && !hasTushareToken()) {
            setTushareToken(oldToken)
            Log.d(TAG, "Migrated Tushare Token from plain text")
        }
    }

    /**
     * 通用加密字符串存储
     */
    fun setString(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }

    /**
     * 通用加密字符串获取
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return securePrefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 检查是否有指定的 Key
     */
    fun hasKey(key: String): Boolean {
        return securePrefs.contains(key)
    }

    /**
     * 删除指定的 Key
     */
    fun remove(key: String) {
        securePrefs.edit().remove(key).apply()
    }
}
