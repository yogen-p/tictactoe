package com.mryogip.plugins

import com.mryogip.models.TicTacToe
import com.mryogip.socket
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(game: TicTacToe) {
    routing {
        socket(game)
    }
}
