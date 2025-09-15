package io.ktor

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureOpenApi()
    configureSerialization()
    configureSecurity()
    configureRouting()
}
