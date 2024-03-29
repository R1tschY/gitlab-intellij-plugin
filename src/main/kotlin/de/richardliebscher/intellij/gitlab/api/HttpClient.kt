package de.richardliebscher.intellij.gitlab.api

import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.ThrowableConvertor
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.api.graphql.GraphQlClientException
import de.richardliebscher.intellij.gitlab.exceptions.GitLabException
import java.io.IOException
import java.io.InputStream

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
    fun readBodyToString(): String
}

interface HttpRequest<T> {
    val location: String

    @Throws(IOException::class)
    fun readContent(response: HttpResponse): T
}

interface JsonRequest<T : Any> {
    val location: String
    val parameters: Map<String, String>? get() = null

    fun deserialize(rawResponse: String, serializer: JsonSerializer): T
}


interface HttpClient {
    /**
     * Set customizer for all requests of this HTTP client.
     */
    fun setSessionCustomizer(customizer: HttpRequestCustomizer)

    @Throws(IOException::class)
    fun <T> execute(
        request: HttpRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer = NoopHttpRequestCustomizer()
    ): T

    @Throws(IOException::class)
    fun <T : Any> execute(
        request: JsonRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer = NoopHttpRequestCustomizer()
    ): T

    /**
     * Executes [GraphQLClientRequest] and returns corresponding [GraphQLClientResponse].
     */
    @Throws(IOException::class)
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