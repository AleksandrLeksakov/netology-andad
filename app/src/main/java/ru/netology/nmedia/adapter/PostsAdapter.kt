package ru.netology.nmedia.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.view.loadCircleCrop
import java.text.SimpleDateFormat
import java.util.*

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener,
) : PagingDataAdapter<Post, PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        Log.d("ADAPTER_DEBUG", "🆕 onCreateViewHolder called")
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        Log.d("ADAPTER_DEBUG", "🔧 onBindViewHolder position: $position, post: ${post?.id ?: "NULL"}")

        if (post != null) {
            holder.bind(post)
        } else {
            Log.w("ADAPTER_DEBUG", "⚠️ Post is NULL at position $position")
        }
    }

    // Добавим метод для отслеживания количества элементов
    override fun getItemCount(): Int {
        val count = super.getItemCount()
        Log.d("ADAPTER_DEBUG", "📊 getItemCount: $count")
        return count
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        Log.d("VIEWHOLDER_DEBUG", "🎨 Binding post ID: ${post.id}, author: ${post.author}")

        binding.apply {
            // Временно добавим цвет фона для видимости
            root.setBackgroundColor(0xFFF8F8F8.toInt())
            root.setPadding(32, 32, 32, 32)

            author.text = post.author
            published.text = formatDate(post.published) // Исправляем формат даты
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = "${post.likes}"

            // Временно сделаем все видимым для отладки
            author.visibility = View.VISIBLE
            published.visibility = View.VISIBLE
            content.visibility = View.VISIBLE
            like.visibility = View.VISIBLE
            menu.visibility = View.VISIBLE
            share.visibility = View.VISIBLE
            avatar.visibility = View.VISIBLE

            // Загружаем аватар с проверкой URL
            val avatarUrl = "${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}"
            Log.d("VIEWHOLDER_DEBUG", "🖼️ Loading avatar from: $avatarUrl")
            avatar.loadCircleCrop(avatarUrl)

            menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE

            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    menu.setGroupVisible(R.id.owned, post.ownedByMe)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }

            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }

            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }

            // Логируем детали поста
            Log.d("VIEWHOLDER_DEBUG", "📝 Post content: ${post.content.take(30)}...")
            Log.d("VIEWHOLDER_DEBUG", "👤 Author: ${post.author}, Likes: ${post.likes}")
        }

        Log.d("VIEWHOLDER_DEBUG", "✅ Post binding completed")
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val date = Date(timestamp * 1000)
            val formatter = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            "Invalid date"
        }
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}