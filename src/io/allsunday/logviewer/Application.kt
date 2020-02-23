package io.allsunday.logviewer

import io.allsunday.logviewer.libs.ktor.JacksonConverter
import io.allsunday.logviewer.pojos.*
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
            call.respond(HttpStatusCode.BadRequest, cause)
        }

        exception<Throwable> { cause ->
            call.respondTextWriter(status = HttpStatusCode.InternalServerError) {
                cause.printStackTrace(PrintWriter(this))
            }
            throw cause
        }
    }

    install(ContentNegotiation) {
        val jsonConverter = JacksonConverter()
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

            get("/services") {
                call.respond(PagedDto(listOf(SERVICE_NAME), total = 1, limit = 1))
            }

            get("/services/{service}/operations") {
                call.respond(PagedDto(emptyList<String>(), total = 0, limit = 0))
            }

            get("/traces") {
                val limit = call.request.queryParameters["limit"]?.toInt() ?: 20

                call.respond(pagedTraces(repo.listTraces(limit)))
            }

            get("/traces/{traceId}") {
                val traceId = call.parameters["traceId"]?.toLong() ?: throw MissingRequestParameterException("traceId")
                call.respond(pagedTraces(repo.getTrace(traceId)))
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
                call.respondText("success")
            }

            post("/collect/spans") {
                val spans = call.receive<List<Span>>()
                repo.addOrUpdateSpans(spans)
                call.respondText("success")
            }
            post("/collect/log") {
                val log = call.receive<Log>()
                repo.addLog(log)
                call.respondText("success")
            }
            post("/collect/logs") {
                val logs = call.receive<List<Log>>()
                repo.addLogs(logs)
                call.respondText("success")
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
