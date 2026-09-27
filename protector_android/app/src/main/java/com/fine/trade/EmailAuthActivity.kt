package com.fine.trade

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fine.trade.databinding.ActivityEmailAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Separate email + password login screen. */
class EmailAuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmailAuthBinding
    private val cloud = CloudSyncClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmailAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (cloud.isLoggedIn()) {
            openBackup()
            return
        }

        binding.btnLogin.setOnClickListener { submitLogin() }
        binding.btnGoSignup.setOnClickListener { finish() }
    }

    private fun submitLogin() {
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()

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
                withContext(Dispatchers.IO) { cloud.signIn(email, password) }
                Toast.makeText(this@EmailAuthActivity, "Logged in", Toast.LENGTH_SHORT).show()
                openBackup()
            } catch (e: Exception) {
                Toast.makeText(
                    this@EmailAuthActivity,
                    e.message ?: "Login failed",
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
        binding.btnLogin.isEnabled = !loading
        binding.btnGoSignup.isEnabled = !loading
    }
}
