package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.Club
import kotlinx.coroutines.flow.Flow

interface ClubRepository {
    fun getPublicClubs(): Flow<List<Club>>
    fun getClubFlow(clubId: String): Flow<Club?>
    fun getClubsForUser(uid: String): Flow<List<Club>>
    suspend fun createClub(club: Club): Result<String>
    suspend fun joinClub(clubId: String): Result<Unit>
    suspend fun leaveClub(clubId: String): Result<Unit>
    suspend fun searchClubs(query: String): List<Club>
}
