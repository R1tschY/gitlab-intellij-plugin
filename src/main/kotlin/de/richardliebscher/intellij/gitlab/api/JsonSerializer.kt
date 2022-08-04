package de.richardliebscher.intellij.gitlab.api

import kotlin.reflect.KClass

interface JsonSerializer {

    fun <T : Any> deserializeList(rawResponse: String, responseType: KClass<T>): List<T>
}