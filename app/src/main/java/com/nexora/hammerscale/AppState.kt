package com.nexora.hammerscale

import com.nexora.hammerscale.model.ConnectionViewModel

enum class GameMode(
    val packageName: String,
    val displayName: String,
    val shortName: String
) {
    SF3("com.nekki.shadowfight3",   "SHADOW FIGHT 3",     "SF3"),
    SFA("com.nekki.shadowfightarena","SHADOW FIGHT ARENA", "SFA")
}

object AppState {
    val viewModel: ConnectionViewModel by lazy { ConnectionViewModel() }

    @Volatile var currentGame: GameMode = GameMode.SF3
}
