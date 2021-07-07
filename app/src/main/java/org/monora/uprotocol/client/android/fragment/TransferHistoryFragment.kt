/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListTransferBinding
import org.monora.uprotocol.client.android.viewholder.TransferViewHolder
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.TransfersViewModel

@AndroidEntryPoint
class TransferHistoryFragment : Fragment(R.layout.layout_transfer_history) {
    private val viewModel: TransfersViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = TransferHistoryAdapter() {
            findNavController().navigate(
                TransferHistoryFragmentDirections.actionTransferHistoryFragmentToTransferDetailsFragment(it)
            )
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_listEmptyTransfer)
        emptyView.emptyImage.setImageResource(R.drawable.ic_compare_arrows_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        viewModel.transfers.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }
    }
}

class TransferHistoryAdapter(
    private val clickListener: (Transfer) -> Unit
) : ListAdapter<Transfer, ViewHolder>(TransferItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == VIEW_TYPE_TRANSFER) {
            return TransferViewHolder(
                ListTransferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        } else {
            throw UnsupportedOperationException()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is TransferViewHolder) {
            holder.bind(getItem(position), clickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_TRANSFER
    }

    companion object {
        const val VIEW_TYPE_TRANSFER = 0
    }
}

class TransferItemCallback : DiffUtil.ItemCallback<Transfer>() {
    override fun areItemsTheSame(oldItem: Transfer, newItem: Transfer): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Transfer, newItem: Transfer): Boolean {
        return oldItem == newItem
    }
}
