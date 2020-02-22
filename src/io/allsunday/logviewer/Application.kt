package io.allsunday.logviewer

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import io.allsunday.logviewer.libs.ktor.JacksonConverter
import io.allsunday.logviewer.pojos.Cursor
import io.allsunday.logviewer.pojos.Error
import io.allsunday.logviewer.pojos.Log
import io.allsunday.logviewer.pojos.Span
import io.allsunday.logviewer.repositories.SpanRepository
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.MissingRequestParameterException
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.getValue
import org.slf4j.event.Level
import java.io.PrintWriter

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(StatusPages) {
        exception<MissingRequestParameterException> { cause ->
            call.respond(HttpStatusCode.BadRequest, Error(cause.message))
        }

        exception<Throwable> { cause ->
            call.respondTextWriter(status = HttpStatusCode.InternalServerError) {
                cause.printStackTrace(PrintWriter(this))
            }
            throw cause
        }
    }

    install(ContentNegotiation) {
        val jsonConverter = JacksonConverter {
            propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        }
        register(ContentType.Application.Json, jsonConverter)
    }

    SpanRepository.initDatabase("jdbc:mysql://localhost:3306/logviewer", "logviewer", "logviewer")
    val repo = SpanRepository()

    routing {
        get("/") {
            call.respondText("HELLO WORLD!")
        }

        route("/api") {
            get("/") {
                call.respond("")
            }

            get("/list-traces") {
                val (cursor, size) = call.getCursorAndSize()
                val ret = repo.pagedTraces(cursor, size)
                call.respond(ret)
            }

            get("/list-spans") {
                val traceId: Long by call.request.queryParameters
                val (cursor, size) = call.getCursorAndSize()
                val ret = repo.pagedTraceSpans(traceId, cursor, size)
                call.respond(ret)
            }

            get("/list-logs") {
                val traceId: Long by call.request.queryParameters
                val (cursor, size) = call.getCursorAndSize()
                val ret = repo.pagedTraceLogs(traceId, cursor, size)
                call.respond(ret)
            }

            post("/collect/span") {
                val span = call.receive<Span>()
                repo.addOrUpdateSpan(span)
                call.respond("")
            }

            post("/collect/log") {
                val log = call.receive<Log>()
                repo.addLog(log)
                call.respond("")
            }
        }
    }
}

fun ApplicationCall.getCursorAndSize(): Pair<Cursor<Long>?, Int> {
    val params = request.queryParameters
    val size = params["size"]?.toIntOrNull() ?: 20
    val cursor = params["cursor"]?.toLongOrNull()?.let { Cursor(it) }
    return cursor to size
}
