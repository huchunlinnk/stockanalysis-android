package com.example.stockanalysis.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stockanalysis.data.model.FundamentalData
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 基本面数据 DAO
 * 
 * 提供基本面数据的增删改查操作
 */
@Dao
interface FundamentalDao {
    
    // ============ 查询操作 ============
    
    /**
     * 获取所有基本面数据
     */
    @Query("SELECT * FROM fundamental_data ORDER BY updateTime DESC")
    fun getAllFundamentalData(): Flow<List<FundamentalData>>
    
    /**
     * 获取所有基本面数据（同步）
     */
    @Query("SELECT * FROM fundamental_data ORDER BY updateTime DESC")
    suspend fun getAllFundamentalDataSync(): List<FundamentalData>
    
    /**
     * 根据股票代码获取基本面数据
     */
    @Query("SELECT * FROM fundamental_data WHERE symbol = :symbol LIMIT 1")
    suspend fun getFundamentalDataBySymbol(symbol: String): FundamentalData?
    
    /**
     * 根据股票代码获取基本面数据（LiveData）
     */
    @Query("SELECT * FROM fundamental_data WHERE symbol = :symbol LIMIT 1")
    fun getFundamentalDataBySymbolLive(symbol: String): LiveData<FundamentalData?>
    
    /**
     * 根据股票代码获取基本面数据（Flow）
     */
    @Query("SELECT * FROM fundamental_data WHERE symbol = :symbol LIMIT 1")
    fun getFundamentalDataBySymbolFlow(symbol: String): Flow<FundamentalData?>
    
    /**
     * 获取指定股票的基本面数据，如果不存在或已过期则返回null
     */
    @Query("SELECT * FROM fundamental_data WHERE symbol = :symbol AND isCacheValid = 1 LIMIT 1")
    suspend fun getValidFundamentalData(symbol: String): FundamentalData?
    
    /**
     * 检查基本面数据是否存在
     */
    @Query("SELECT EXISTS(SELECT 1 FROM fundamental_data WHERE symbol = :symbol)")
    suspend fun exists(symbol: String): Boolean
    
    /**
     * 检查缓存是否有效（1天内）
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM fundamental_data 
            WHERE symbol = :symbol 
            AND isCacheValid = 1 
            AND updateTime > :validTime
        )
    """)
    suspend fun isCacheValid(symbol: String, validTime: Date): Boolean
    
    /**
     * 获取已过期的基本面数据
     */
    @Query("SELECT * FROM fundamental_data WHERE updateTime < :expireTime")
    suspend fun getExpiredData(expireTime: Date): List<FundamentalData>
    
    /**
     * 获取基本面数据数量
     */
    @Query("SELECT COUNT(*) FROM fundamental_data")
    suspend fun getCount(): Int
    
    /**
     * 获取指定股票的基本面数据数量
     */
    @Query("SELECT COUNT(*) FROM fundamental_data WHERE symbol = :symbol")
    suspend fun getCountBySymbol(symbol: String): Int
    
    // ============ 插入操作 ============
    
    /**
     * 插入基本面数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFundamentalData(data: FundamentalData)
    
    /**
     * 批量插入基本面数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFundamentalDataList(dataList: List<FundamentalData>)
    
    // ============ 更新操作 ============
    
    /**
     * 更新基本面数据
     */
    @Update
    suspend fun updateFundamentalData(data: FundamentalData)
    
    /**
     * 标记缓存为无效
     */
    @Query("UPDATE fundamental_data SET isCacheValid = 0 WHERE symbol = :symbol")
    suspend fun invalidateCache(symbol: String)
    
    /**
     * 标记所有缓存为无效
     */
    @Query("UPDATE fundamental_data SET isCacheValid = 0")
    suspend fun invalidateAllCache()
    
    /**
     * 更新缓存状态
     */
    @Query("UPDATE fundamental_data SET isCacheValid = :isValid WHERE symbol = :symbol")
    suspend fun updateCacheStatus(symbol: String, isValid: Boolean)
    
    /**
     * 更新更新时间
     */
    @Query("UPDATE fundamental_data SET updateTime = :updateTime WHERE symbol = :symbol")
    suspend fun updateTime(symbol: String, updateTime: Date)
    
    // ============ 删除操作 ============
    
    /**
     * 删除基本面数据
     */
    @Delete
    suspend fun deleteFundamentalData(data: FundamentalData)
    
    /**
     * 根据股票代码删除基本面数据
     */
    @Query("DELETE FROM fundamental_data WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)
    
    /**
     * 删除所有基本面数据
     */
    @Query("DELETE FROM fundamental_data")
    suspend fun deleteAll()
    
    /**
     * 删除过期的基本面数据
     */
    @Query("DELETE FROM fundamental_data WHERE updateTime < :expireTime")
    suspend fun deleteExpiredData(expireTime: Date)
    
    // ============ 搜索和筛选 ============
    
    /**
     * 根据股票名称搜索
     */
    @Query("SELECT * FROM fundamental_data WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<FundamentalData>
    
    /**
     * 获取最近更新的数据
     */
    @Query("SELECT * FROM fundamental_data ORDER BY updateTime DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int): List<FundamentalData>
    
    /**
     * 获取需要更新的数据（超过指定天数）
     */
    @Query("SELECT * FROM fundamental_data WHERE updateTime < :thresholdTime OR isCacheValid = 0")
    suspend fun getDataNeedUpdate(thresholdTime: Date): List<FundamentalData>
}
