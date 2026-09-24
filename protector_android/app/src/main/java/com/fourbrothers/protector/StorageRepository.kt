package com.fourbrothers.protector

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class StorageRepository(context: Context) {
    private val prefs = context.getSharedPreferences("protector_app", Context.MODE_PRIVATE)

    enum class SearchFilter { BOTH, PROTECTOR, MODEL }

    fun loadProtectors(): MutableList<Protector> {
        val raw = prefs.getString(KEY_PROTECTORS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val models = o.optJSONArray("mobileModels") ?: JSONArray()
                Protector(
                    id = o.getString("id"),
                    protectorName = o.getString("protectorName"),
                    mobileModels = List(models.length()) { j -> models.getString(j) }
                )
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveProtectors(list: List<Protector>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("protectorName", p.protectorName)
                    .put("mobileModels", JSONArray(p.mobileModels))
            )
        }
        prefs.edit().putString(KEY_PROTECTORS, arr.toString()).apply()
    }

    fun loadMobileModels(): MutableList<MobileModel> {
        val raw = prefs.getString(KEY_MODELS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MobileModel(o.getString("id"), o.getString("name"))
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveMobileModels(list: List<MobileModel>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("id", m.id).put("name", m.name))
        }
        prefs.edit().putString(KEY_MODELS, arr.toString()).apply()
    }

    fun ensureMobileModel(raw: String): String {
        val name = raw.trim()
        require(name.isNotEmpty()) { "Mobile model cannot be empty" }
        val models = loadMobileModels()
        val existing = models.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.name
        models.add(MobileModel(UUID.randomUUID().toString(), name))
        models.sortBy { it.name.lowercase() }
        saveMobileModels(models)
        return name
    }

    fun suggest(query: String): List<String> {
        val q = query.trim().lowercase()
        val all = loadMobileModels().map { it.name }
        if (q.isEmpty()) return all.take(20)
        return all.filter { it.lowercase().contains(q) }
    }

    fun searchSuggestions(query: String, filter: SearchFilter): List<SuggestionItem> {
        val q = query.trim().lowercase()
        val protectors = loadProtectors()
        val protectorNames = protectors.map { it.protectorName }.toMutableSet()
        val modelNames = linkedSetOf<String>().apply {
            loadMobileModels().map { it.name }.forEach { add(it) }
            protectors.flatMap { it.mobileModels }.forEach { add(it) }
        }

        val map = linkedMapOf<String, SuggestionItem.Kind>()
        fun put(name: String, kind: SuggestionItem.Kind) {
            val key = name.trim()
            if (key.isEmpty()) return
            val existing = map[key]
            map[key] = when {
                existing == null -> kind
                existing == kind -> existing
                else -> SuggestionItem.Kind.BOTH
            }
        }

        when (filter) {
            SearchFilter.PROTECTOR -> protectorNames.forEach { put(it, SuggestionItem.Kind.PROTECTOR) }
            SearchFilter.MODEL -> modelNames.forEach { put(it, SuggestionItem.Kind.MODEL) }
            SearchFilter.BOTH -> {
                protectorNames.forEach { put(it, SuggestionItem.Kind.PROTECTOR) }
                modelNames.forEach { put(it, SuggestionItem.Kind.MODEL) }
            }
        }

        val list = map.entries
            .map { SuggestionItem(it.key, it.value) }
            .sortedBy { it.label.lowercase() }

        if (q.isEmpty()) return list.take(20)
        return list.filter { it.label.lowercase().contains(q) }.take(20)
    }

    fun modelSuggestions(query: String): List<SuggestionItem> {
        return searchSuggestions(query, SearchFilter.MODEL)
    }

    fun createProtector(name: String, models: List<String>): Protector {
        val cleaned = normalizeModels(models)
        val resolved = cleaned.map { ensureMobileModel(it) }
        val protector = Protector(UUID.randomUUID().toString(), name.trim(), resolved)
        val list = loadProtectors()
        list.add(protector)
        saveProtectors(list)
        return protector
    }

    fun updateProtector(id: String, name: String, models: List<String>): Protector {
        val cleaned = normalizeModels(models)
        val resolved = cleaned.map { ensureMobileModel(it) }
        val list = loadProtectors()
        val index = list.indexOfFirst { it.id == id }
        require(index >= 0) { "Protector not found" }
        val updated = Protector(id, name.trim(), resolved)
        list[index] = updated
        saveProtectors(list)
        return updated
    }

    fun deleteProtector(id: String) {
        val list = loadProtectors().filterNot { it.id == id }
        saveProtectors(list)
    }

    fun search(query: String, filter: SearchFilter = SearchFilter.BOTH): List<Protector> {
        val q = query.trim().lowercase()
        val all = loadProtectors()
        if (q.isEmpty()) return all
        return all.filter { p ->
            val nameHit = p.protectorName.lowercase().contains(q)
            val modelHit = p.mobileModels.any { it.lowercase().contains(q) }
            when (filter) {
                SearchFilter.PROTECTOR -> nameHit
                SearchFilter.MODEL -> modelHit
                SearchFilter.BOTH -> nameHit || modelHit
            }
        }
    }

    fun shareText(p: Protector): String {
        val models = p.mobileModels.mapIndexed { i, m -> "  ${i + 1}. $m" }.joinToString("\n")
        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("  FOUR BROTHERS")
            appendLine("  PROTECTOR CARD")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("Protector")
            appendLine(p.protectorName)
            appendLine()
            appendLine("Fits these mobile models")
            appendLine(models.ifBlank { "  - Not set" })
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Shared from FOUR BROTHERS")
        }
    }

    private fun normalizeModels(models: List<String>): List<String> {
        val cleaned = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        models.forEach { raw ->
            val name = raw.trim()
            if (name.isEmpty()) return@forEach
            val key = name.lowercase()
            require(key !in seen) { "Duplicate mobile model: $name" }
            seen.add(key)
            cleaned.add(name)
        }
        require(cleaned.isNotEmpty()) { "Add at least one mobile model" }
        return cleaned
    }

    companion object {
        private const val KEY_PROTECTORS = "protectors"
        private const val KEY_MODELS = "mobile_models"
    }
}
