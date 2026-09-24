package com.fourbrothers.protector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.fourbrothers.protector.databinding.ActivityFormBinding

class FormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFormBinding
    private lateinit var repo: StorageRepository
    private val modelRows = mutableListOf<AutoCompleteTextView>()
    private val suggestionAdapters = mutableMapOf<AutoCompleteTextView, SuggestionAdapter>()
    private var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = StorageRepository(this)

        editId = intent.getStringExtra(EXTRA_ID)
        binding.toolbar.title = if (editId == null) "Add Protector" else "Edit Protector"
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnAddModel.setOnClickListener { addModelRow("") }
        binding.btnSave.setOnClickListener { save() }

        if (editId != null) {
            val existing = repo.loadProtectors().firstOrNull { it.id == editId }
            if (existing != null) {
                binding.nameInput.setText(existing.protectorName)
                existing.mobileModels.forEach { addModelRow(it) }
            } else {
                addModelRow("")
            }
        } else {
            addModelRow("")
        }
    }

    private fun addModelRow(value: String) {
        val row = LayoutInflater.from(this)
            .inflate(R.layout.item_model_input, binding.modelsContainer, false)
        val input = row.findViewById<AutoCompleteTextView>(R.id.modelInput)
        val remove = row.findViewById<ImageButton>(R.id.btnRemove)
        val adapter = SuggestionAdapter(this)
        suggestionAdapters[input] = adapter
        input.setAdapter(adapter)
        input.threshold = 1
        input.setDropDownBackgroundResource(R.drawable.bg_suggestion_popup)
        input.setText(value)
        refreshSuggestions(input)
        input.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position) ?: return@setOnItemClickListener
            input.setText(item.label)
            input.setSelection(item.label.length)
        }
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                refreshSuggestions(input)
                alignDropdown(input)
            }
        }
        input.setOnClickListener {
            refreshSuggestions(input)
            alignDropdown(input)
        }
        input.addTextChangedListener(SimpleWatcher {
            refreshSuggestions(input)
        })
        remove.setOnClickListener {
            if (modelRows.size <= 1) {
                Toast.makeText(this, "At least one mobile model is required", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            binding.modelsContainer.removeView(row)
            modelRows.remove(input)
            suggestionAdapters.remove(input)
        }
        binding.modelsContainer.addView(row)
        modelRows.add(input)
    }

    private fun alignDropdown(input: AutoCompleteTextView) {
        input.post {
            val parent = input.parent as? ViewGroup
            val width = parent?.width ?: input.width
            if (width > 0) input.dropDownWidth = width
        }
    }

    private fun refreshSuggestions(input: AutoCompleteTextView) {
        val adapter = suggestionAdapters[input] ?: return
        val suggestions = repo.modelSuggestions(input.text?.toString().orEmpty())
        adapter.replaceAll(suggestions)
        alignDropdown(input)
        if (suggestions.isNotEmpty() && (input.hasFocus() || input.isPopupShowing)) {
            input.showDropDown()
        }
    }

    private fun save() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Protector name is required", Toast.LENGTH_SHORT).show()
            return
        }
        val models = modelRows.map { it.text?.toString().orEmpty() }
        try {
            if (editId == null) {
                repo.createProtector(name, models)
            } else {
                repo.updateProtector(editId!!, name, models)
            }
            Toast.makeText(
                this,
                if (editId == null) "Protector saved" else "Protector updated",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Could not save", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_ID = "protector_id"
    }
}

private class SimpleWatcher(val onChange: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(s: android.text.Editable?) = onChange()
}
