package com.fine.trade

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fine.trade.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Account / Sign-up screen for logged-out users:
 * Continue with Google + email sign-up form.
 * Login is on [EmailAuthActivity].
 */
class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private val cloud = CloudSyncClient()

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        lifecycleScope.launch {
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                    ?: throw IllegalStateException(
                        "Google ID token missing. Add SHA-1 in Firebase and set google_web_client_id."
                    )
                setLoading(true)
                withContext(Dispatchers.IO) {
                    cloud.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                }
                Toast.makeText(this@AuthActivity, "Signed in with Google", Toast.LENGTH_SHORT).show()
                openBackup()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AuthActivity,
                    e.message ?: "Google sign-in failed",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (cloud.isLoggedIn()) {
            openBackup()
            return
        }

        binding.btnGoogle.setOnClickListener { startGoogleSignIn() }
        binding.btnSignup.setOnClickListener { submitSignup() }
        binding.btnGoLogin.setOnClickListener {
            startActivity(Intent(this, EmailAuthActivity::class.java))
        }
    }

    private fun startGoogleSignIn() {
        val webClientId = getString(R.string.google_web_client_id)
        if (webClientId.startsWith("REPLACE_")) {
            Toast.makeText(
                this,
                "Set google_web_client_id in res/values/strings.xml (Firebase Web client ID).",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        val client = GoogleSignIn.getClient(this, options)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun submitSignup() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (name.length < 2) {
            Toast.makeText(this, "Enter your full name", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { cloud.signUp(name, email, password) }
                Toast.makeText(this@AuthActivity, "Account created", Toast.LENGTH_SHORT).show()
                openBackup()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AuthActivity,
                    e.message ?: "Sign up failed",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun openBackup() {
        startActivity(Intent(this, BackupActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnGoogle.isEnabled = !loading
        binding.btnSignup.isEnabled = !loading
        binding.btnGoLogin.isEnabled = !loading
    }
}
