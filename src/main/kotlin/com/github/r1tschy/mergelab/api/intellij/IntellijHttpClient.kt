package com.github.r1tschy.mergelab.api.intellij

import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.expediagroup.graphql.client.types.GraphQLClientRequest
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.r1tschy.mergelab.api.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.ThrowableConvertor
import com.intellij.util.io.HttpRequests
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLConnection


private class JavaHttpRequestBuilder(private val connection: URLConnection) : HttpRequestBuilder {
    override fun addHeader(key: String, value: String) {
        connection.addRequestProperty(key, value)
    }
}


class IntellijHttpResponse(private val request: HttpRequests.Request, private val indicator: ProgressIndicator?) :
    HttpResponse {
    override fun getHeader(key: String): String? {
        return request.connection.getHeaderField(key)
    }

    override fun <T> readBody(converter: ThrowableConvertor<InputStream, T, IOException>): T {
        val contentLength = request.connection.contentLengthLong
        val inputStream = if (contentLength > 0 && indicator != null) {
            ProgressMonitoredInputStream(request.inputStream, indicator, contentLength)
        } else {
            request.inputStream
        }

        return inputStream.use { converter.convert(it) }
    }

    override fun readBodyToString(): String {
        return request.readString(indicator)
    }
}

class IntellijHttpClient(
    private val url: String,
    private val serializer: GraphQLClientSerializer,
    private val restSerializer: JsonSerializer,
) : HttpClient {
    private val sessionCustomizers: MutableList<HttpRequestCustomizer> = mutableListOf()

    override fun setSessionCustomizer(customizer: HttpRequestCustomizer) {
        sessionCustomizers.add(customizer)
    }

    override fun <T> execute(
        request: HttpRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer
    ): T {
        return HttpRequests.request("${request.url ?: url}${request.path}")
            .userAgent("MergeLab plugin for IntelliJ IDEA")
            .tuner { connection ->
                val httpRequest = JavaHttpRequestBuilder(connection)
                sessionCustomizers.forEach { it.customize(httpRequest) }
                requestCustomizer.customize(httpRequest)
            }
            .connect {
                val connection = it.connection as HttpURLConnection
                // TODO check content type / encoding
                if (connection.responseCode >= 400) {
                    // TODO error
                }

                request.readContent(IntellijHttpResponse(it, progressIndicator))
            }
    }

    override fun <T : Any> execute(
        request: JsonRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer
    ): T {
        return execute(
            object : HttpRequest<T> {
                override val path: String
                    get() = request.path
                override val url: String?
                    get() = request.url

                override fun readContent(response: HttpResponse): T {
                    return request.deserialize(response.readBodyToString(), restSerializer)
                }
            },
            progressIndicator,
            requestCustomizer
        )
    }

    override fun <T : Any> query(
        request: GraphQLClientRequest<T>,
        progressIndicator: ProgressIndicator,
        requestCustomizer: HttpRequestCustomizer
    ): GraphQLClientResponse<T> {
        val rawResult: String = HttpRequests.post("$url/api/graphql", JSON_MIME_TYPE)
            .userAgent("MergeLab plugin for IntelliJ IDEA")
            .tuner { connection ->
                connection.setRequestProperty("Accept", JSON_MIME_TYPE)

                val httpRequest = JavaHttpRequestBuilder(connection)
                sessionCustomizers.forEach { it.customize(httpRequest) }
                requestCustomizer.customize(httpRequest)
            }
            .connect {
                val connection = it.connection as HttpURLConnection
                it.write(serializer.serialize(request))

                if (connection.responseCode >= 400) {
                    // TODO error
                }

                it.readString()
            }

        return serializer.deserialize(rawResult, request.responseType())
    }
}

class ProgressMonitoredInputStream(
    private val inputStream: InputStream,
    private val indicator: ProgressIndicator,
    private val length: Long
) : InputStream() {
    private var seen: Long = 0

    override fun read(): Int {
        val byte = inputStream.read()
        if (byte >= 0) {
            update(1)
        }
        return byte
    }

    override fun read(b: ByteArray): Int {
        return inputStream.read(b).also { update(it.toLong()) }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return inputStream.read(b, off, len).also { update(it.toLong()) }
    }

    override fun close() {
        inputStream.close()
    }

    override fun skip(n: Long): Long {
        return inputStream.skip(n).also(::update)
    }

    override fun available(): Int {
        return inputStream.available()
    }

    private fun update(inc: Long) {
        indicator.checkCanceled()
        if (inc > 0) {
            seen += inc
            if (!indicator.isIndeterminate) {
                indicator.fraction = seen.toDouble() / length
            }
        }
    }
}


