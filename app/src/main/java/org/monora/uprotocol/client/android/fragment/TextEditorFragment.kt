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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.monora.android.codescanner.BarcodeEncoder
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import javax.inject.Inject

@AndroidEntryPoint
class TextEditorFragment : Fragment(R.layout.layout_text_editor), SnackbarPlacementProvider {
    @Inject
    lateinit var sharedTextRepository: SharedTextRepository

    private var sharedText: SharedText? = null

    private val text
        get() = requireView().findViewById<EditText>(R.id.editText).text.toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editText = view.findViewById<EditText>(R.id.editText)
        val text = requireActivity().intent?.let {
            if (it.hasExtra(TextEditorActivity.EXTRA_TEXT_MODEL)) {
                sharedText = it.getParcelableExtra(TextEditorActivity.EXTRA_TEXT_MODEL)
                return@let sharedText?.text
            } else if (it.hasExtra(TextEditorActivity.EXTRA_TEXT)) {
                return@let it.getStringExtra(TextEditorActivity.EXTRA_TEXT)
            }

            null
        }
        val backPressedDispatcher = requireActivity().onBackPressedDispatcher
        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                val hasObject = sharedText != null

                when {
                    checkDeletionNeeded() -> {
                        createSnackbar(R.string.ques_deleteEmptiedText)
                            .setAction(R.string.butn_delete) {
                                removeText()
                                backPressedDispatcher.onBackPressed()
                            }
                            .show()
                    }
                    checkSaveNeeded() -> {
                        createSnackbar(if (hasObject) R.string.mesg_clipboardUpdateNotice else R.string.mesg_textSaveNotice)
                            .setAction(if (hasObject) R.string.butn_update else R.string.butn_save) {
                                saveText()
                                backPressedDispatcher.onBackPressed()
                            }
                            .show()
                    }
                    else -> {
                        isEnabled = false
                        backPressedDispatcher.onBackPressed()
                    }
                }

                lifecycleScope.launch {
                    isEnabled = false
                    delay(3000)
                    isEnabled = true
                }
            }
        }

        editText.addTextChangedListener {
            backPressedCallback.isEnabled = checkDeletionNeeded() || checkSaveNeeded()
        }

        backPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        text?.let {
            editText.text.apply {
                clear()
                append(text)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_text_editor, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_action_save)
            .setVisible(!checkDeletionNeeded()).isEnabled = checkSaveNeeded()
        menu.findItem(R.id.menu_action_remove).isVisible = sharedText != null
        menu.findItem(R.id.menu_action_show_as_qr_code).isEnabled = (text.length in 1..1200)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_action_save) {
            saveText()
            createSnackbar(R.string.mesg_textStreamSaved).show()
        } else if (id == R.id.menu_action_copy) {
            (requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("copiedText", text))
            createSnackbar(R.string.mesg_textCopiedToClipboard).show()
        } else if (id == R.id.menu_action_share) {
            val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/*")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)))
        } else if (id == R.id.menu_action_share_trebleshot) {
            findNavController().navigate(TextEditorFragmentDirections.pickClient())
        } else if (id == R.id.menu_action_show_as_qr_code) {
            if (text.length in 1..1200) {
                val formatWriter = MultiFormatWriter()
                try {
                    val bitMatrix: BitMatrix = formatWriter.encode(text, BarcodeFormat.QR_CODE, 800, 800)
                    val encoder = BarcodeEncoder()
                    val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
                    val dialog = BottomSheetDialog(requireActivity())
                    val view = LayoutInflater.from(requireActivity()).inflate(
                        R.layout.layout_show_text_as_qr_code, null
                    )
                    val qrImage = view.findViewById<ImageView>(R.id.layout_show_text_as_qr_code_image)
                    GlideApp.with(this)
                        .load(bitmap)
                        .into(qrImage)
                    val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    dialog.setTitle(R.string.butn_showAsQrCode)
                    dialog.setContentView(view, params)
                    dialog.show()
                } catch (e: WriterException) {
                    Toast.makeText(requireContext(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                }
            }
        } else if (id == R.id.menu_action_remove) {
            removeText()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    private fun removeText() {
        sharedText?.let {
            lifecycle.coroutineScope.launch {
                sharedTextRepository.delete(it)
                sharedText = null
            }
        }
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(requireView(), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    private fun checkDeletionNeeded(): Boolean {
        val editorText: String = text
        return editorText.isEmpty() && !sharedText?.text.isNullOrEmpty()
    }

    private fun checkSaveNeeded(): Boolean {
        val editorText: String = text
        return editorText.isNotEmpty() && editorText != sharedText?.text
    }

    private fun saveText() {
        val date = System.currentTimeMillis()
        var update = false
        val item = this.sharedText?.also {
            it.modified = date
            it.text = text
            update = true
        } ?: SharedText(0, text, date).also {
            this.sharedText = it
        }

        lifecycleScope.launch {
            if (update) sharedTextRepository.update(item) else sharedTextRepository.insert(item)
        }
    }
}