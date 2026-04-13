package com.vorsaciew.app.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vorsaciew.app.core.util.toRelativeTime
import com.vorsaciew.app.data.model.Post

@Composable
fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clickable { onAuthorClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (post.authorAvatarUrl.isNotEmpty()) {
                AsyncImage(model = post.authorAvatarUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape))
            } else {
                Surface(Modifier.size(36.dp), shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Person, null, Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(post.authorName.ifEmpty { "Driver" },
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(post.createdAt.toRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Media
        if (post.mediaUrls.isNotEmpty()) {
            AsyncImage(model = post.mediaUrls.first(), contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        }

        // Caption
        if (post.caption.isNotEmpty()) {
            Text(post.caption,
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium)
        }

        // Action bar
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            IconButton(onClick = onLike) {
                Icon(Icons.Default.FavoriteBorder, "Like",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("${post.likeCount}", Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onComment) {
                Icon(Icons.Default.ChatBubbleOutline, "Comment")
            }
            Text("${post.commentCount}", Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}
