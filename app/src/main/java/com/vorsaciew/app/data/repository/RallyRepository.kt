package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.Rally
import kotlinx.coroutines.flow.Flow

interface RallyRepository {
    fun getPublicRallies(): Flow<List<Rally>>
    fun getRallyFlow(rallyId: String): Flow<Rally?>
    suspend fun createRally(rally: Rally): Result<String>
    suspend fun joinRally(rallyId: String): Result<Unit>
    suspend fun leaveRally(rallyId: String): Result<Unit>
}
