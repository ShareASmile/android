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

package org.monora.uprotocol.client.android.fragment.content.transfer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.content.Video
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.Progress
import org.monora.uprotocol.client.android.util.Transfers
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingViewModel
import org.monora.uprotocol.client.android.viewmodel.consume
import org.monora.uprotocol.core.transfer.TransferItem
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class PrepareIndexFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var factory: PrepareIndexViewModel.Factory

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    private val viewModel: PrepareIndexViewModel by viewModels {
        PrepareIndexViewModel.ModelFactory(factory, selectionViewModel.getSelections())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_prepare_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is PreparationState.Preparing -> {
                }
                is PreparationState.Ready -> findNavController().navigate(
                    PrepareIndexFragmentDirections.actionPrepareIndexFragmentToSendFragment(
                        it.groupId, it.list.toTypedArray()
                    )
                )
            }
        }
    }
}

class PrepareIndexViewModel @AssistedInject internal constructor(
    @ApplicationContext _context: Context,
    @Assisted private val list: List<Any>,
) : ViewModel() {
    private val context = WeakReference(_context)

    private val _state = MutableLiveData<PreparationState>()

    val state = liveData {
        emitSource(_state)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.postValue(PreparationState.Preparing)

            val groupId = Random.nextLong()
            val items = mutableListOf<UTransferItem>()
            val progress = Progress(list.size)
            val type = TransferItem.Type.Outgoing

            list.forEach {
                if (it is FileModel) context.get()?.let { context ->
                    Transfers.createStructure(context, items, progress, groupId, it.file) { progress, file ->

                    }
                } else {
                    progress.index += 1
                    val id = progress.index.toLong()
                    val item = when (it) {
                        is Song -> UTransferItem(
                            id, groupId, it.displayName, it.mimeType, it.size, null, it.uri.toString(), type
                        )
                        is Image -> UTransferItem(
                            id, groupId, it.displayName, it.mimeType, it.size, null, it.uri.toString(), type
                        )
                        is Video -> UTransferItem(
                            id, groupId, it.displayName, it.mimeType, it.size, null, it.uri.toString(), type
                        )
                        else -> {
                            progress.index -= 1
                            Log.e(TAG, "Unknown object type was given ${it.javaClass.simpleName}")
                            return@forEach
                        }
                    }

                    items.add(item)
                }
            }

            _state.postValue(PreparationState.Ready(groupId, items))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(selections: List<Any>): PrepareIndexViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val selections: List<Any>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(PrepareIndexViewModel::class.java)) {
                "Requested unknown view model type"
            }

            return factory.create(selections) as T
        }
    }

    companion object {
        private const val TAG = "PrepareIndexViewModel"
    }
}

sealed class PreparationState {
    object Preparing : PreparationState()

    class Ready(val groupId: Long, val list: List<UTransferItem>) : PreparationState()
}