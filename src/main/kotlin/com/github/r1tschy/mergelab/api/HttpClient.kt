package com.github.r1tschy.mergelab.api

import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.api.graphql.GraphQlClientException
import com.github.r1tschy.mergelab.exceptions.GitLabException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.ThrowableConvertor
import java.io.IOException
import java.io.InputStream
import kotlin.reflect.KClass

const val JSON_MIME_TYPE = "application/json"


interface HttpRequestBuilder {
    fun addHeader(key: String, value: String)
}


interface HttpRequestCustomizer {
    fun customize(request: HttpRequestBuilder)
}

class NoopHttpRequestCustomizer : HttpRequestCustomizer {
    override fun customize(request: HttpRequestBuilder) {}
}

class BearerAuthorization(private val token: GitlabAccessToken) : HttpRequestCustomizer {
    override fun customize(request: HttpRequestBuilder) {
        request.addHeader("Authorization", "Bearer ${token.asString()}")
    }
}

interface HttpResponse {
    fun getHeader(key: String): String?
    fun <T> readBody(converter: ThrowableConvertor<InputStream, T, IOException>): T
}

interface HttpRequest<T> {
    val url: String? get() = null
    val path: String get() = ""

    @Throws(IOException::class)
    fun readContent(response: HttpResponse): T
}

interface GetJsonRequest<T : Any> {
    val parameters: Map<String, String>? get() = null

    fun responseType(): KClass<T>
}


interface HttpClient {
    /**
     * Set customizer for all requests of this HTTP client.
     */
    fun setSessionCustomizer(customizer: HttpRequestCustomizer)

    fun <T> execute(
        request: HttpRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer = NoopHttpRequestCustomizer()
    ): T

    /**
     * Executes [GraphQLClientRequest] and returns corresponding [GraphQLClientResponse].
     */
    fun <T : Any> query(
        request: GraphQLClientRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer = NoopHttpRequestCustomizer()
    ): GraphQLClientResponse<T>
}


@Throws(GraphQlClientException::class)
fun <T> GraphQLClientResponse<T>.check(): T {
    val errors = this.errors
    if (errors != null) {
        throw GraphQlClientException(errors)
    } else {
        val data = this.data
        if (data != null) {
            return this.data!!
        } else {
            throw GitLabException("Invalid GraphQL response: data and errors are null")
        }
    }
}