package com.example.stockanalysis.data.local

import androidx.room.TypeConverter
import com.example.stockanalysis.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Room 类型转换器
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromMarketType(value: MarketType): String {
        return value.name
    }

    @TypeConverter
    fun toMarketType(value: String): MarketType {
        return MarketType.valueOf(value)
    }

    @TypeConverter
    fun fromDecision(value: Decision): String {
        return value.name
    }

    @TypeConverter
    fun toDecision(value: String): Decision {
        return Decision.valueOf(value)
    }

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String {
        return value.name
    }

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel {
        return ConfidenceLevel.valueOf(value)
    }

    @TypeConverter
    fun fromRiskLevel(value: RiskLevel): String {
        return value.name
    }

    @TypeConverter
    fun toRiskLevel(value: String): RiskLevel {
        return RiskLevel.valueOf(value)
    }

    @TypeConverter
    fun fromTechnicalAnalysis(value: TechnicalAnalysis?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toTechnicalAnalysis(value: String?): TechnicalAnalysis? {
        return value?.let {
            val type = object : TypeToken<TechnicalAnalysis>() {}.type
            gson.fromJson<TechnicalAnalysis>(it, type)
        }
    }

    @TypeConverter
    fun fromFundamentalAnalysis(value: FundamentalAnalysis?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toFundamentalAnalysis(value: String?): FundamentalAnalysis? {
        return value?.let {
            val type = object : TypeToken<FundamentalAnalysis>() {}.type
            gson.fromJson<FundamentalAnalysis>(it, type)
        }
    }

    @TypeConverter
    fun fromSentimentAnalysis(value: SentimentAnalysis?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toSentimentAnalysis(value: String?): SentimentAnalysis? {
        return value?.let {
            val type = object : TypeToken<SentimentAnalysis>() {}.type
            gson.fromJson<SentimentAnalysis>(it, type)
        }
    }

    @TypeConverter
    fun fromRiskAssessment(value: RiskAssessment?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toRiskAssessment(value: String?): RiskAssessment? {
        return value?.let {
            val type = object : TypeToken<RiskAssessment>() {}.type
            gson.fromJson<RiskAssessment>(it, type)
        }
    }

    @TypeConverter
    fun fromActionPlan(value: ActionPlan?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toActionPlan(value: String?): ActionPlan? {
        return value?.let {
            val type = object : TypeToken<ActionPlan>() {}.type
            gson.fromJson<ActionPlan>(it, type)
        }
    }

    @TypeConverter
    fun fromNewsItemList(value: List<NewsItem>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toNewsItemList(value: String?): List<NewsItem>? {
        return value?.let {
            val type = object : TypeToken<List<NewsItem>>() {}.type
            gson.fromJson<List<NewsItem>>(it, type)
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(it, type)
        }
    }

    @TypeConverter
    fun fromCheckItemList(value: List<CheckItem>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCheckItemList(value: String?): List<CheckItem>? {
        return value?.let {
            val type = object : TypeToken<List<CheckItem>>() {}.type
            gson.fromJson<List<CheckItem>>(it, type)
        }
    }
}
