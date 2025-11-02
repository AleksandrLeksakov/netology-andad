package ru.netology.nmedia.model

import androidx.paging.PagingData
import ru.netology.nmedia.dto.Post

data class FeedModelState(
    val posts: PagingData<Post> = PagingData.empty(),
    val loading: Boolean = false,
    val error: Boolean = false,
    val refreshing: Boolean = false,
)
