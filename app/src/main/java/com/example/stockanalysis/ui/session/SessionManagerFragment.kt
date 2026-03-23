package com.example.stockanalysis.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockanalysis.R
import com.example.stockanalysis.data.model.ChatSession
import com.example.stockanalysis.databinding.FragmentSessionManagerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 会话管理Fragment
 */
@AndroidEntryPoint
class SessionManagerFragment : Fragment() {

    private var _binding: FragmentSessionManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionViewModel by viewModels()
    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            onSessionClick = { session ->
                // TODO: 打开会话详情或恢复分析
                Toast.makeText(requireContext(), "打开会话: ${session.title}", Toast.LENGTH_SHORT).show()
            },
            onMoreClick = { session, view ->
                showSessionMenu(session, view)
            }
        )

        binding.rvSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSessions.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            viewModel.searchSessions(query)
        }
    }

    private fun setupFab() {
        binding.fabNewSession.setOnClickListener {
            // TODO: 创建新会话
            Toast.makeText(requireContext(), "创建新会话", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessions.collect { sessions ->
                adapter.submitList(sessions)

                if (sessions.isEmpty()) {
                    binding.rvSessions.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvSessions.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
            }
        }
    }

    private fun showSessionMenu(session: ChatSession, anchorView: View) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.session_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rename -> {
                    showRenameDialog(session)
                    true
                }
                R.id.action_export -> {
                    exportSession(session)
                    true
                }
                R.id.action_delete -> {
                    deleteSession(session)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showRenameDialog(session: ChatSession) {
        // TODO: 显示重命名对话框
        Toast.makeText(requireContext(), "重命名: ${session.title}", Toast.LENGTH_SHORT).show()
    }

    private fun exportSession(session: ChatSession) {
        viewLifecycleOwner.lifecycleScope.launch {
            val markdown = viewModel.exportSessionAsMarkdown(session.id)
            // TODO: 分享或保存Markdown
            Toast.makeText(requireContext(), "导出成功", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSession(session: ChatSession) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteSession(session.id)
            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
