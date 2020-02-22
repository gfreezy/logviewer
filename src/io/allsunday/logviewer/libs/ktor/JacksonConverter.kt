package io.allsunday.logviewer.libs.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.allsunday.logviewer.pojos.Ok
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.ContentConverter
import io.ktor.features.suitableCharset
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.request.contentCharset
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream

@Suppress("BlockingMethodInNonBlockingContext")
class JacksonConverter(
    private val objectmapper: ObjectMapper = jacksonObjectMapper(),
    configure: ObjectMapper.() -> Unit = {}
) : ContentConverter {
    init {
        objectmapper.configure()
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any? {
        return TextContent(
            objectmapper.writeValueAsString(Ok(value)),
            contentType.withCharset(context.call.suitableCharset())
        )
    }

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val type = request.type
        val value = request.value as? ByteReadChannel ?: return null
        val reader = value.toInputStream().reader(context.call.request.contentCharset() ?: Charsets.UTF_8)
        return objectmapper.readValue(reader, type.javaObjectType)
    }
}
