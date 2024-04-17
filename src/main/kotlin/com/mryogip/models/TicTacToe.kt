package com.mryogip.models

import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TicTacToe {

    private val state = MutableStateFlow(GameState())
    private val playerSockets = ConcurrentHashMap<Char, WebSocketSession>()
    private val gameScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var newGameJob: Job? = null

    init {
        state.onEach(::broadcast).launchIn(gameScope)
    }

    fun connectPlayer(session: WebSocketSession): Char? {
        val isPlayerX = state.value.connectedPlayers.any { it == 'X' }
        val player = if (isPlayerX) 'O' else 'X'

        state.update {
            if (state.value.connectedPlayers.contains(player)) {
                return null
            }

            if (!playerSockets.contains(player)) {
                playerSockets[player] = session
            }

            it.copy(
                connectedPlayers = it.connectedPlayers + player
            )
        }

        return player
    }

    fun disconnectPlayer(player: Char) {
        playerSockets.remove(player)

        state.update {
            it.copy(
                connectedPlayers = it.connectedPlayers - player
            )
        }
    }

    fun finishTurn(player: Char, x: Int, y: Int) {
        if (x == -1 && y == -1) {
            startNewRound()
            return
        }

        if (state.value.field[y][x] != null || state.value.winningPlayer != null) {
            return
        }

        if (state.value.playerInTurn != player) {
            return
        }

        val currentPlayer = state.value.playerInTurn
        state.update {
            val newField = it.field.also { field ->
                field[y][x] = currentPlayer
            }

            val isBoardFull = newField.all { field -> field.all { value -> value != null } }
            if (isBoardFull) {
                startNewRoundDelayed()
            }

            it.copy(
                field = newField,
                isBoardFull = isBoardFull,
                playerInTurn = if (currentPlayer == 'X') 'O' else 'X',
                winningPlayer = getWinningPlayer()?.also {
                    startNewRoundDelayed()
                }
            )
        }
    }

    private suspend fun broadcast(state: GameState) {
        playerSockets.values.forEach { socket ->
            socket.send(Json.encodeToString(state))
        }
    }

    private fun getWinningPlayer(): Char? {
        val field = state.value.field
        return if (field[0][0] != null && field[0][0] == field[0][1] && field[0][1] == field[0][2]) {
            field[0][0]
        } else if (field[1][0] != null && field[1][0] == field[1][1] && field[1][1] == field[1][2]) {
            field[1][0]
        } else if (field[2][0] != null && field[2][0] == field[2][1] && field[2][1] == field[2][2]) {
            field[2][0]
        } else if (field[0][0] != null && field[0][0] == field[1][0] && field[1][0] == field[2][0]) {
            field[0][0]
        } else if (field[0][1] != null && field[0][1] == field[1][1] && field[1][1] == field[2][1]) {
            field[0][1]
        } else if (field[0][2] != null && field[0][2] == field[1][2] && field[1][2] == field[2][2]) {
            field[0][2]
        } else if (field[0][0] != null && field[0][0] == field[1][1] && field[1][1] == field[2][2]) {
            field[0][0]
        } else if (field[0][2] != null && field[0][2] == field[1][1] && field[1][1] == field[2][0]) {
            field[0][2]
        } else null
    }

    private fun startNewRoundDelayed() {
        newGameJob?.cancel()
        newGameJob = gameScope.launch {
            delay(5000L)
            startNewRound()
        }
    }

    private fun startNewRound() {
        state.update {
            it.copy(
                playerInTurn = 'X',
                isBoardFull = false,
                winningPlayer = null,
                field = GameState.emptyField()
            )
        }
    }
}