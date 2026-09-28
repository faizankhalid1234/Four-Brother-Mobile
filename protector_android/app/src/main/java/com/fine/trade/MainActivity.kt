package com.fine.trade

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.fine.trade.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: StorageRepository
    private lateinit var adapter: ProtectorAdapter
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var filter = StorageRepository.SearchFilter.BOTH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiCompat.setupActivityWindow(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = StorageRepository(this)
        suggestionAdapter = SuggestionAdapter(this)
        adapter = ProtectorAdapter(
            emptyList(),
            onShare = { shareProtector(it) },
            onEdit = { p ->
                startActivity(
                    Intent(this, FormActivity::class.java).putExtra(FormActivity.EXTRA_ID, p.id)
                )
            },
            onDelete = { p -> confirmDelete(p) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        binding.recycler.setHasFixedSize(false)

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, FormActivity::class.java))
        }
        binding.btnAccount.setOnClickListener {
            val dest = if (CloudSyncClient().isLoggedIn()) {
                BackupActivity::class.java
            } else {
                AuthActivity::class.java
            }
            startActivity(Intent(this, dest))
        }
        binding.btnShareAll.setOnClickListener {
            StockShareHelper.shareAllAsTable(this, repo)
        }

        binding.filterGroup.setOnCheckedStateChangeListener { _, _ ->
            filter = when {
                binding.chipProtector.isChecked -> StorageRepository.SearchFilter.PROTECTOR
                binding.chipModel.isChecked -> StorageRepository.SearchFilter.MODEL
                else -> StorageRepository.SearchFilter.BOTH
            }
            updateSearchHint()
            refreshSuggestions()
            refresh(currentQuery())
        }

        setupSearch()
        applySafeInsets()
        updateSearchHint()
        binding.versionLabel.text = UiCompat.appVersionText(this)
    }

    private fun setupSearch() {
        val search = binding.searchInput
        search.threshold = 1
        search.setAdapter(suggestionAdapter)
        search.setDropDownBackgroundResource(R.drawable.bg_suggestion_popup)
        search.dropDownVerticalOffset = resources.getDimensionPixelSize(R.dimen.screen_pad) / 4
        // Selecting a suggestion calls setText before onItemClick. Defer suggestion
        // rebuild so the click can still resolve the chosen row safely.
        val suggestionRefresh = Runnable { refreshSuggestions() }
        search.setOnItemClickListener { _, view, position, _ ->
            val item = suggestionAdapter.getItem(position)
                ?: view?.tag as? SuggestionItem
                ?: return@setOnItemClickListener
            search.removeCallbacks(suggestionRefresh)
            search.setText(item.label)
            search.setSelection(item.label.length)
            search.dismissDropDown()
            refresh(item.label)
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                search.removeCallbacks(suggestionRefresh)
                search.post(suggestionRefresh)
                refresh(s?.toString().orEmpty())
            }
        })
        search.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                refreshSuggestions()
                alignDropdownWidth()
            }
        }
        search.setOnClickListener {
            refreshSuggestions()
            alignDropdownWidth()
        }
    }

    private fun applySafeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun alignDropdownWidth() {
        binding.searchLayout.post {
            val width = binding.searchLayout.width
            if (width > 0) {
                binding.searchInput.dropDownWidth = width
            } else {
                binding.searchInput.dropDownWidth = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSuggestions()
        refresh(currentQuery())
    }

    private fun currentQuery(): String = binding.searchInput.text?.toString().orEmpty()

    private fun updateSearchHint() {
        binding.searchLayout.hint = when (filter) {
            StorageRepository.SearchFilter.PROTECTOR -> "Search by protector name…"
            StorageRepository.SearchFilter.MODEL -> "Search by mobile model…"
            StorageRepository.SearchFilter.BOTH -> "Search protector or mobile model…"
        }
    }

    private fun refreshSuggestions() {
        val suggestions = repo.searchSuggestions(currentQuery(), filter)
        suggestionAdapter.replaceAll(suggestions)
        val ac = binding.searchInput
        alignDropdownWidth()
        if (suggestions.isNotEmpty() && (ac.hasFocus() || ac.isPopupShowing)) {
            ac.showDropDown()
        }
    }

    private fun refresh(query: String) {
        val items = repo.search(query, filter)
        adapter.submit(items)
        val total = repo.loadProtectors().size
        binding.countBadge.text = if (query.isBlank()) {
            "$total in stock"
        } else {
            "${items.size} found"
        }

        val empty = items.isEmpty()
        binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (empty) View.GONE else View.VISIBLE
        binding.emptyText.text = if (query.isBlank()) {
            "No Protectors Found\n\nTap + Add Protector to create your first Protector."
        } else {
            "No matches found\n\nTry another name, or change the filter chips above."
        }
    }

    private fun shareProtector(p: Protector) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Protector: ${p.protectorName}")
            putExtra(Intent.EXTRA_TEXT, repo.shareText(p))
        }
        startActivity(Intent.createChooser(send, "Share protector via"))
    }

    private fun confirmDelete(p: Protector) {
        AlertDialog.Builder(this)
            .setTitle("Delete Protector?")
            .setMessage("Delete \"${p.protectorName}\"? Mobile models will stay in master data.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                repo.deleteProtector(p.id)
                refreshSuggestions()
                refresh(currentQuery())
                Snackbar.make(binding.root, "Protector deleted", Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }
}
