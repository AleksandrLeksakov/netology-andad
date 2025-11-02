package ru.netology.nmedia.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.CancellationException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException

class PostPagingSource(
    private val service: ApiService,
) : PagingSource<Long, Post>() {

    override fun getRefreshKey(state: PagingState<Long, Post>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            android.util.Log.d("PAGING_DEBUG", "Loading with params: $params")

            val response = when (params) {
                is LoadParams.Refresh -> {
                    // Первая загрузка или refresh - загружаем самые новые посты
                    service.getLatest(params.loadSize)
                }
                is LoadParams.Append -> {
                    // Загрузка более старых постов
                    val key = params.key ?: return LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                    service.getBefore(key, params.loadSize)
                }
                is LoadParams.Prepend -> {
                    // Загрузка более новых постов (обычно не используется в нашем случае)
                    return LoadResult.Page(
                        data = emptyList(),
                        prevKey = params.key,
                        nextKey = null
                    )
                }
            }

            if (!response.isSuccessful) {
                android.util.Log.e("PAGING_DEBUG", "API error: ${response.code()} - ${response.message()}")
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(
                response.code(),
                response.message(),
            )

            android.util.Log.d("PAGING_DEBUG", "Loaded ${body.size} posts")

            // Логика ключей для пагинации
            val nextKey = if (body.isEmpty()) {
                null // Больше нет данных
            } else {
                body.last().id // ID последнего поста для следующей страницы
            }

            val prevKey = when (params) {
                is LoadParams.Append -> params.key
                else -> null
            }

            return LoadResult.Page(
                data = body,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: CancellationException) {
            android.util.Log.d("PAGING_DEBUG", "Load cancelled")
            throw e // Пробрасываем отмену выше
        } catch (e: IOException) {
            android.util.Log.e("PAGING_DEBUG", "Network error: $e")
            return LoadResult.Error(NetworkError)
        } catch (e: Exception) {
            android.util.Log.e("PAGING_DEBUG", "Unknown error: $e")
            return LoadResult.Error(UnknownError)
        }
    }
}