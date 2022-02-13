package com.github.r1tschy.mergelab.api.restV4

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.r1tschy.mergelab.api.JsonSerializer
import kotlin.reflect.KClass

fun getObjectMapper(): ObjectMapper {
    return jacksonObjectMapper()
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

class JacksonJsonSerializer(private val mapper: ObjectMapper = getObjectMapper()) : JsonSerializer {
    override fun <T : Any> deserializeList(rawResponse: String, responseType: KClass<T>): List<T> {
        return mapper.readValue(
            rawResponse,
            mapper.typeFactory.constructParametricType(List::class.java, responseType.java))
    }
}