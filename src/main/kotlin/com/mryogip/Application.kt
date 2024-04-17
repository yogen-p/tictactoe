package com.mryogip

import com.mryogip.models.TicTacToe
import com.mryogip.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val game = TicTacToe()
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureRouting(game)
}
