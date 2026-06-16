package com.example.data

import com.example.TravelPlan
import com.example.service.TravelService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TravelRepository(
    private val travelPlanDao: TravelPlanDao,
    private val travelService: TravelService
) {
    val allSavedPlans: kotlinx.coroutines.flow.Flow<List<SavedTravelPlanEntity>> = travelPlanDao.getAllSavedPlans()

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
        coerceInputValues = true 
    }

    suspend fun generateTravelPlan(
        departure: String,
        destination: String,
        dates: String,
        group: String,
        budget: String,
        style: String,
        extra: String
    ): Result<TravelPlan> {
        val result = travelService.generateTravelPlan(departure, destination, dates, group, budget, style, extra)
        result.onSuccess { plan ->
            saveCachedPlan(plan)
            saveToHistory(plan, dates)
        }
        return result
    }

    suspend fun generateSuggestions(homeCity: String): Result<List<com.example.DestinationSuggestion>> {
        return travelService.generateSuggestions(homeCity)
    }

    suspend fun saveToHistory(plan: TravelPlan, dates: String) {
        try {
            val jsonStr = json.encodeToString(plan)
            travelPlanDao.insertSavedPlan(
                SavedTravelPlanEntity(
                    destination = if (plan.destination.isNotBlank()) plan.destination else "Unbekannt",
                    dates = dates,
                    totalBudget = if (plan.totalBudget.isNotBlank()) plan.totalBudget else "",
                    planData = jsonStr
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSavedPlan(id: Int) {
        try {
            travelPlanDao.deleteSavedPlanById(id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveCachedPlan(plan: TravelPlan) {
        try {
            val jsonStr = json.encodeToString(plan)
            travelPlanDao.insertPlan(TravelPlanEntity(planData = jsonStr))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getCachedPlan(): TravelPlan? {
        val entity = travelPlanDao.getCachedPlan() ?: return null
        return try {
            json.decodeFromString<TravelPlan>(entity.planData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
