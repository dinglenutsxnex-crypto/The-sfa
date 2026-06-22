package com.nexora.hammerscale

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.nexora.hammerscale.databinding.ActivityMainBinding
import com.nexora.hammerscale.model.ConnectionViewModel
import com.nexora.hammerscale.model.ConnectionViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ConnectionViewModel

    private val VPN_REQUEST_CODE     = 100
    private val OVERLAY_REQUEST_CODE = 101

    // Which game the user last tapped — set before VPN permission request
    private var pendingGame: GameMode = GameMode.SF3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ConnectionViewModelFactory())[ConnectionViewModel::class.java]

        binding.btnPlaySf3.setOnClickListener {
            handlePlayButton(GameMode.SF3)
        }

        binding.btnPlaySfa.setOnClickListener {
            handlePlayButton(GameMode.SFA)
        }

        binding.tvDiscordLink.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/AW9vGhVA2j")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }

        viewModel.vpnRunning.observe(this) { running ->
            updateCardStates(running)
        }

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
    }

    private fun handlePlayButton(game: GameMode) {
        if (viewModel.vpnRunning.value == true) {
            if (AppState.currentGame == game) {
                stopVpn()
            } else {
                stopVpn()
            }
        } else {
            pendingGame = game
            requestVpnPermission()
        }
    }

    private fun updateCardStates(running: Boolean) {
        if (running) {
            val activeGame = AppState.currentGame
            // Active game — show pause
            val activeBtn = if (activeGame == GameMode.SF3) binding.btnPlaySf3 else binding.btnPlaySfa
            val activeStatus = if (activeGame == GameMode.SF3) binding.tvSf3Status else binding.tvSfaStatus
            activeBtn.setImageResource(android.R.drawable.ic_media_pause)
            activeStatus.text = "● live"
            activeStatus.setTextColor(0xFF3FB950.toInt())

            // Idle game — greyed out play
            val idleBtn = if (activeGame == GameMode.SF3) binding.btnPlaySfa else binding.btnPlaySf3
            val idleStatus = if (activeGame == GameMode.SF3) binding.tvSfaStatus else binding.tvSf3Status
            idleBtn.setImageResource(R.drawable.ic_play)
            idleBtn.alpha = 0.4f
            idleStatus.text = "idle"
            idleStatus.setTextColor(0xFF8B949E.toInt())
        } else {
            // Both idle
            binding.btnPlaySf3.apply { setImageResource(R.drawable.ic_play); alpha = 1f }
            binding.btnPlaySfa.apply { setImageResource(R.drawable.ic_play); alpha = 1f }
            binding.tvSf3Status.apply { text = "idle"; setTextColor(0xFF8B949E.toInt()) }
            binding.tvSfaStatus.apply { text = "idle"; setTextColor(0xFF8B949E.toInt()) }
        }
    }

    // ── Overlay ───────────────────────────────────────────────────────────

    private fun startOverlay() {
        startService(Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START
        })
        Handler(Looper.getMainLooper()).postDelayed({
            launchTargetApp()
        }, 500)
    }

    private fun stopOverlay() {
        startService(Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        })
    }

    private fun requestOverlayPermission() {
        startActivityForResult(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            OVERLAY_REQUEST_CODE
        )
    }

    private fun launchTargetApp() {
        val intent = packageManager.getLaunchIntentForPackage(AppState.currentGame.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    // ── VPN ───────────────────────────────────────────────────────────────

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, VPN_REQUEST_CODE)
        else startVpn()
    }

    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            VPN_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK) startVpn()
            OVERLAY_REQUEST_CODE -> {}
        }
    }

    private fun startVpn() {
        AppState.currentGame = pendingGame
        startService(Intent(this, TrafficVpnService::class.java).apply {
            action = TrafficVpnService.ACTION_START
        })
        if (Settings.canDrawOverlays(this)) {
            Handler(Looper.getMainLooper()).postDelayed({
                startOverlay()
            }, 300)
        }
    }

    private fun stopVpn() {
        stopOverlay()
        startService(Intent(this, TrafficVpnService::class.java).apply {
            action = TrafficVpnService.ACTION_STOP
        })
    }
}
