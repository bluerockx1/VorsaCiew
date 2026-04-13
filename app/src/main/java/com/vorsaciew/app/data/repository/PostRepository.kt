package com.vorsaciew.app.data.repository

import com.vorsaciew.app.data.model.Post
import com.vorsaciew.app.data.model.PostComment
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getFeedPosts(): Flow<List<Post>>
    fun getPostsForUser(uid: String): Flow<List<Post>>
    fun getPostsForClub(clubId: String): Flow<List<Post>>
    fun getPostFlow(postId: String): Flow<Post?>
    fun getComments(postId: String): Flow<List<PostComment>>
    suspend fun createPost(post: Post): Result<String>
    suspend fun toggleLike(postId: String): Result<Unit>
    suspend fun addComment(postId: String, comment: PostComment): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>
}
