package io.ktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable

fun Application.configureRouting() {
    routing {
        authenticate("auth-oauth-google") {
            /**
             * Authenticate.
             */
            get("/login") {
                call.respondRedirect("/callback")
            }

            /**
             * Oauth callback endpoint.
             */
            get("/callback") {
                val principal: OAuthAccessTokenResponse.OAuth2? = call.authentication.principal()
                call.sessions.set(UserSession(principal?.accessToken.toString()))
                call.respondRedirect("/hello")
            }

            /**
             * Hello, world.
             *
             * @response 200 text/plaintext Hello
             */
            get("/hello") {
                call.respondText("Hello")
            }

            /**
             * Data back-end.
             */
            route("/data") {
                route("/users") {
                    listCrud(mutableListOf(User(1u, "John")))
                }
                route("/messages") {
                    listCrud(mutableListOf(Message(1u, "Hello World")))
                }
            }
        }

        openAPI("/docs")
    }
}

inline fun <reified E: Entity> Route.listCrud(list: MutableList<E>) {
    /**
     * Get a single entity by ID.
     *
     * @path id [ULong] the ID of the entity
     * @response 400 The ID parameter is malformatted or missing.
     * @response 404 The entity for the given ID does not exist.
     * @response 200 [E] The entity found with the given ID.
     */
    get("/{id}") {
        val id = call.parameters["id"]?.toULongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val entity = list.find { it.id == id }
            ?: return@get call.respond(HttpStatusCode.NotFound)
        call.respond(entity)
    }
    /**
     * Get a list of [E].
     *
     * @response 200 [E]+ The list of items.
     */
    get {
        call.respond(list)
    }
    /**
     * Save a new [E].
     *
     * @response 204 The new entity was saved.
     */
    post {
        list += call.receive<E>()
        call.respond(HttpStatusCode.NoContent)
    }
    /**
     * Delete the entity with the given ID.
     *
     * @path id [ULong] the ID of the entity to remove
     * @response 400 The ID parameter is malformatted or missing.
     * @response 404 The entity for the given ID does not exist.
     * @response 204 The entity was deleted.
     */
    delete("/{id}") {
        val id = call.parameters["id"]?.toULongOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest)
        if (!list.removeIf { it.id == id })
            return@delete call.respond(HttpStatusCode.NotFound)
        call.respond(HttpStatusCode.NoContent)
    }
}

interface Entity {
    val id: ULong
}

@Serializable
data class User(override val id: ULong, val name: String): Entity

@Serializable
data class Message(override val id: ULong, val text: String): Entity