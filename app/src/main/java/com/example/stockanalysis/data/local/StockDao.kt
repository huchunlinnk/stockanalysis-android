package com.example.stockanalysis.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.stockanalysis.data.model.Stock

/**
 * 股票 DAO
 */
@Dao
interface StockDao {
    
    @Query("SELECT * FROM stocks ORDER BY sortOrder ASC, addedTime DESC")
    fun getAllStocks(): LiveData<List<Stock>>
    
    @Query("SELECT * FROM stocks ORDER BY sortOrder ASC, addedTime DESC")
    suspend fun getAllStocksSync(): List<Stock>
    
    @Query("SELECT * FROM stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getStockBySymbol(symbol: String): Stock?
    
    @Query("SELECT * FROM stocks WHERE isFavorite = 1 ORDER BY sortOrder ASC")
    fun getFavoriteStocks(): LiveData<List<Stock>>
    
    @Query("SELECT COUNT(*) FROM stocks")
    suspend fun getStockCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM stocks WHERE symbol = :symbol)")
    suspend fun isStockExists(symbol: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: Stock)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<Stock>)
    
    @Update
    suspend fun updateStock(stock: Stock)
    
    @Delete
    suspend fun deleteStock(stock: Stock)
    
    @Query("DELETE FROM stocks WHERE symbol = :symbol")
    suspend fun deleteStockBySymbol(symbol: String)
    
    @Query("DELETE FROM stocks")
    suspend fun deleteAllStocks()
    
    @Query("UPDATE stocks SET sortOrder = :order WHERE symbol = :symbol")
    suspend fun updateSortOrder(symbol: String, order: Int)
    
    @Query("UPDATE stocks SET isFavorite = :isFavorite WHERE symbol = :symbol")
    suspend fun updateFavoriteStatus(symbol: String, isFavorite: Boolean)
    
    @Query("SELECT * FROM stocks WHERE name LIKE '%' || :query || '%' OR symbol LIKE '%' || :query || '%'")
    suspend fun searchStocks(query: String): List<Stock>
}
