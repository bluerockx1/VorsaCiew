package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserFlow(uid: String): Flow<User?>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun followUser(targetUid: String): Result<Unit>
    suspend fun unfollowUser(targetUid: String): Result<Unit>
    fun getFollowers(uid: String): Flow<List<User>>
    fun getFollowing(uid: String): Flow<List<User>>
    suspend fun searchUsers(query: String): List<User>
    suspend fun isFollowing(targetUid: String): Boolean
}
