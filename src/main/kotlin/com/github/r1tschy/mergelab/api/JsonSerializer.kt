package com.github.r1tschy.mergelab.api

import kotlin.reflect.KClass

interface JsonSerializer {

    fun <T : Any> deserializeList(rawResponse: String, responseType: KClass<T>): List<T>
}