package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        android.util.Log.d("FEED_DEBUG", "🟢 FeedFragment onCreateView")

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }

            override fun onLike(post: Post) {
                android.util.Log.d("FEED_DEBUG", "❤️ Like clicked for post ${post.id}")
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                android.util.Log.d("FEED_DEBUG", "🗑️ Remove clicked for post ${post.id}")
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })

        // ✅ ДОБАВЬТЕ ЭТИ ДВЕ СТРОЧКИ - LayoutManager
        binding.list.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.list.adapter = adapter

        android.util.Log.d("FEED_DEBUG", "🔧 LayoutManager set, Adapter set to RecyclerView")

        // 1. Отслеживаем данные из ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                android.util.Log.d("FEED_DEBUG", "🔄 Starting data collection from ViewModel")

                viewModel.data.collectLatest { pagingData ->
                    android.util.Log.d("FEED_DEBUG", "📦 Received PagingData, submitting to adapter")
                    adapter.submitData(pagingData)
                }
            }
        }

        // 2. Детальная отладка состояний загрузки адаптера
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    android.util.Log.d("FEED_DEBUG", "📊 LoadState: $state")

                    val isRefreshing = state.refresh is LoadState.Loading ||
                            state.prepend is LoadState.Loading ||
                            state.append is LoadState.Loading

                    binding.swiperefresh.isRefreshing = isRefreshing

                    // Детальный анализ
                    when (state.refresh) {
                        is LoadState.Loading -> android.util.Log.d("FEED_DEBUG", "🔄 REFRESH_LOADING")
                        is LoadState.NotLoading -> android.util.Log.d("FEED_DEBUG", "✅ REFRESH_NOT_LOADING")
                        is LoadState.Error -> {
                            val error = (state.refresh as LoadState.Error).error
                            android.util.Log.e("FEED_DEBUG", "❌ REFRESH_ERROR: $error")
                        }
                    }
                }
            }
        }

        // 3. Отслеживаем количество элементов в адаптере
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Проверяем snapshot данных каждые 2 секунды
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    val itemCount = adapter.itemCount
                    android.util.Log.d("FEED_DEBUG", "📈 Adapter item count: $itemCount")

                    if (itemCount > 0) {
                        android.util.Log.d("FEED_DEBUG", "🎉 SUCCESS: Posts are in adapter!")
                    }
                }
            }
        }

        binding.swiperefresh.setOnRefreshListener {
            android.util.Log.d("FEED_DEBUG", "🔄 Manual refresh triggered")
            adapter.refresh()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        android.util.Log.d("FEED_DEBUG", "🏁 FeedFragment setup completed")
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.d("FEED_DEBUG", "▶️ FeedFragment onStart")
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("FEED_DEBUG", "▶️ FeedFragment onResume")
    }
}
