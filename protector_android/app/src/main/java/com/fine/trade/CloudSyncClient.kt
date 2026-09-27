package com.fine.trade

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Auth (email/password + Google) and manual cloud sync via n8n → Google Drive.
 * n8n webhook URLs come from Firebase Realtime Database (free Spark plan; change anytime).
 * Local phone JSON remains the source of truth until the user taps Export/Import.
 */
class CloudSyncClient(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean = currentUser != null

    fun displayLabel(): String {
        val user = currentUser ?: return "Not signed in"
        val name = user.displayName?.trim().orEmpty()
        val email = user.email?.trim().orEmpty()
        return when {
            name.isNotEmpty() && email.isNotEmpty() -> "$name ($email)"
            name.isNotEmpty() -> name
            email.isNotEmpty() -> email
            else -> user.uid
        }
    }

    suspend fun signUp(name: String, email: String, password: String): FirebaseUser =
        suspendCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        cont.resumeWithException(IllegalStateException("Sign up failed"))
                        return@addOnSuccessListener
                    }
                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim())
                        .build()
                    user.updateProfile(profile)
                        .addOnSuccessListener { cont.resume(user) }
                        .addOnFailureListener {
                            // Account exists even if name update fails
                            cont.resume(user)
                        }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun signIn(email: String, password: String): FirebaseUser =
        suspendCoroutine { cont ->
            auth.signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { cont.resume(it.user!!) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun signInWithCredential(credential: AuthCredential): FirebaseUser =
        suspendCoroutine { cont ->
            auth.signInWithCredential(credential)
                .addOnSuccessListener { cont.resume(it.user!!) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    fun signOut() = auth.signOut()

    private suspend fun idToken(): String = suspendCoroutine { cont ->
        val user = auth.currentUser
        if (user == null) {
            cont.resumeWithException(IllegalStateException("Please log in first"))
            return@suspendCoroutine
        }
        user.getIdToken(true)
            .addOnSuccessListener { cont.resume(it.token ?: "") }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Reads once from Realtime Database path `config/{key}`. */
    private suspend fun webhookUrl(key: String): String = suspendCoroutine { cont ->
        val ref = database.getReference(CONFIG_PATH).child(key)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val url = snapshot.getValue(String::class.java)?.trim().orEmpty()
                if (url.isEmpty() || url.contains("YOUR_N8N_HOST")) {
                    cont.resumeWithException(
                        IllegalStateException(
                            "Set config/$key in Firebase Realtime Database."
                        )
                    )
                } else {
                    cont.resume(url)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                cont.resumeWithException(
                    IllegalStateException("Failed to load $key: ${error.message}")
                )
            }
        })
    }

    suspend fun exportToDrive(dataJson: String): String {
        val user = auth.currentUser ?: throw IllegalStateException("Please log in first")
        val token = idToken()
        val exportUrl = webhookUrl(KEY_N8N_EXPORT_URL)
        val body = JSONObject()
            .put("action", "export")
            .put("uid", user.uid)
            .put("email", user.email ?: "")
            .put("name", user.displayName ?: "")
            .put("idToken", token)
            .put("fileName", "fine_trade_data.json")
            .put("folderName", "Fine Trade")
            .put("data", JSONObject(dataJson))
            .toString()

        val request = Request.Builder()
            .url(exportUrl)
            .post(body.toRequestBody(JSON))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        return execute(request, "export")
    }

    suspend fun importFromDrive(): String {
        val user = auth.currentUser ?: throw IllegalStateException("Please log in first")
        val token = idToken()
        val importUrl = webhookUrl(KEY_N8N_IMPORT_URL)
        val body = JSONObject()
            .put("action", "import")
            .put("uid", user.uid)
            .put("email", user.email ?: "")
            .put("name", user.displayName ?: "")
            .put("idToken", token)
            .put("fileName", "fine_trade_data.json")
            .put("folderName", "Fine Trade")
            .toString()

        val request = Request.Builder()
            .url(importUrl)
            .post(body.toRequestBody(JSON))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .build()

        val responseText = execute(request, "import")
        val parsed = JSONObject(responseText)
        return when {
            parsed.has("data") && parsed.get("data") is JSONObject ->
                parsed.getJSONObject("data").toString()
            parsed.has("protectors") || parsed.has("mobile_models") ->
                parsed.toString()
            parsed.has("json") && parsed.get("json") is String ->
                parsed.getString("json")
            else -> responseText
        }
    }

    private fun execute(request: Request, action: String): String {
        return try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(text).optString("message").ifBlank {
                            JSONObject(text).optString("error")
                        }
                    }.getOrNull().orEmpty().ifBlank { text.ifBlank { response.message } }
                    throw IllegalStateException("$action failed (${response.code}): $message")
                }
                text.ifBlank { "{}" }
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(friendlyNetworkError(e), e)
        }
    }

    private fun friendlyNetworkError(e: Throwable): String {
        val chain = generateSequence(e) { it.cause }.toList()
        val ssl = chain.any {
            it is SSLHandshakeException ||
                it is SSLPeerUnverifiedException ||
                it is CertificateException ||
                it is CertPathValidatorException ||
                it.message?.contains("Chain validation", ignoreCase = true) == true ||
                it.message?.contains("certificate", ignoreCase = true) == true
        }
        if (ssl) {
            return "SSL certificate error on n8n host (likely expired). " +
                "Renew HTTPS cert for muhammadumersheraz2000.socioglory.com, then try again."
        }
        if (chain.any { it is IOException }) {
            return e.message?.takeIf { it.isNotBlank() }
                ?: "Network error. Check internet and n8n URL."
        }
        return e.message?.takeIf { it.isNotBlank() } ?: "Cloud sync failed"
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /** Firebase Console → Realtime Database → Data tab. */
        const val CONFIG_PATH = "config"
        const val KEY_N8N_EXPORT_URL = "n8n_export_url"
        const val KEY_N8N_IMPORT_URL = "n8n_import_url"
    }
}
