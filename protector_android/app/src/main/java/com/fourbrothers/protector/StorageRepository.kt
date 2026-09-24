package com.fourbrothers.protector

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Saves all protectors + mobile models on the user's phone storage.
 * Primary: app private internal files (survives app updates).
 * Mirror: app-specific external folder when available.
 * Also migrates any older SharedPreferences data once.
 */
class StorageRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val internalFile = File(appContext.filesDir, DATA_FILE_NAME)

    enum class SearchFilter { BOTH, PROTECTOR, MODEL }

    private fun externalMirrorFile(): File? {
        val dir = appContext.getExternalFilesDir(null) ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, DATA_FILE_NAME)
    }

    private fun readJsonFile(file: File): JSONObject? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            JSONObject(file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    private fun loadRoot(): JSONObject {
        readJsonFile(internalFile)?.let { return it }
        externalMirrorFile()?.let { mirror ->
            readJsonFile(mirror)?.let { json ->
                writeRoot(json)
                return json
            }
        }
        // Migrate old SharedPreferences (if any) onto phone file storage.
        val migrated = migrateFromPrefsIfNeeded()
        if (migrated != null) return migrated
        return JSONObject()
            .put(KEY_PROTECTORS, JSONArray())
            .put(KEY_MODELS, JSONArray())
    }

    private fun migrateFromPrefsIfNeeded(): JSONObject? {
        val protectorsRaw = prefs.getString(KEY_PROTECTORS, null)
        val modelsRaw = prefs.getString(KEY_MODELS, null)
        if (protectorsRaw.isNullOrBlank() && modelsRaw.isNullOrBlank()) return null
        val root = JSONObject()
            .put(KEY_PROTECTORS, try { JSONArray(protectorsRaw ?: "[]") } catch (_: Exception) { JSONArray() })
            .put(KEY_MODELS, try { JSONArray(modelsRaw ?: "[]") } catch (_: Exception) { JSONArray() })
        writeRoot(root)
        prefs.edit().remove(KEY_PROTECTORS).remove(KEY_MODELS).apply()
        return root
    }

    private fun writeRoot(root: JSONObject) {
        val text = root.toString()
        // Atomic-ish write to internal phone storage
        val tmp = File(appContext.filesDir, "$DATA_FILE_NAME.tmp")
        tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(internalFile)) {
            internalFile.writeText(text, Charsets.UTF_8)
            tmp.delete()
        }
        // Mirror copy on external app storage (still this phone, survives clearer storage browsability)
        try {
            externalMirrorFile()?.writeText(text, Charsets.UTF_8)
        } catch (_: Exception) {
            // External may be unavailable; internal is enough.
        }
        // Keep a light prefs flag so we know phone storage is active
        prefs.edit().putBoolean(KEY_FILE_STORAGE, true).apply()
    }

    fun storageLocationHint(): String {
        val external = externalMirrorFile()?.absolutePath
        return if (external != null) {
            "Saved on this phone\n$external"
        } else {
            "Saved on this phone\n${internalFile.absolutePath}"
        }
    }

    fun loadProtectors(): MutableList<Protector> {
        val arr = loadRoot().optJSONArray(KEY_PROTECTORS) ?: JSONArray()
        return try {
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
        val root = loadRoot()
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("protectorName", p.protectorName)
                    .put("mobileModels", JSONArray(p.mobileModels))
            )
        }
        root.put(KEY_PROTECTORS, arr)
        writeRoot(root)
    }

    fun loadMobileModels(): MutableList<MobileModel> {
        val arr = loadRoot().optJSONArray(KEY_MODELS) ?: JSONArray()
        return try {
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                MobileModel(o.getString("id"), o.getString("name"))
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveMobileModels(list: List<MobileModel>) {
        val root = loadRoot()
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().put("id", m.id).put("name", m.name))
        }
        root.put(KEY_MODELS, arr)
        writeRoot(root)
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
        private const val PREFS_NAME = "protector_app"
        private const val DATA_FILE_NAME = "four_brothers_data.json"
        private const val KEY_PROTECTORS = "protectors"
        private const val KEY_MODELS = "mobile_models"
        private const val KEY_FILE_STORAGE = "using_phone_file_storage"
    }
}
