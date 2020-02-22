package io.allsunday.logviewer.libs.exposed

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table

inline fun <reified T : Any> Table.jsonb(name: String, jsonMapper: ObjectMapper? = null): Column<T> =
    registerColumn(
        name,
        StringJson(object :
            TypeReference<T>() {}, jsonMapper)
    )

class StringJson<out T : Any>(private val klass: TypeReference<T>, private val jsonMapper: ObjectMapper? = null) :
    ColumnType() {

    private val objectMapper get() = jsonMapper ?: defaultObjectMapper

    override fun sqlType() = "TEXT"

    override fun valueFromDB(value: Any): Any {
        value as String
        return try {
            objectMapper.readValue(value, klass)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Can't parse JSON: $value")
        }
    }

    override fun notNullValueToDB(value: Any): Any = objectMapper.writeValueAsString(value)
    override fun nonNullValueToString(value: Any): String = "'${objectMapper.writeValueAsString(value)}'"

    companion object {
        val defaultObjectMapper = jacksonObjectMapper()
    }
}
