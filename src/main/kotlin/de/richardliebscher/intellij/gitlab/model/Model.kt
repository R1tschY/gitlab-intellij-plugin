// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.model

import com.intellij.collaboration.api.ServerPath
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import de.richardliebscher.intellij.gitlab.exceptions.GitLabIllegalUrlException
import java.util.regex.Pattern

private const val HTTPS_DEFAULT_PORT = 443
private const val HTTP_DEFAULT_PORT = 80


const val DEFAULT_HOST: String = "gitlab.com"
const val DEFAULT_URL: String = "https://$DEFAULT_HOST"
val DEFAULT_SERVER_URL: GitLabServerUrl = GitLabServerUrl.DEFAULT

const val SERVICE_DISPLAY_NAME: String = "GitLab"

/**
 * URL to a GitLab instance.
 */
@Tag("server")
data class GitLabServerUrl(
    @Attribute("https")
    val https: Boolean,
    @Attribute("host")
    val host: String,
    @Attribute("port")
    val port: Int = defaultPort(https)
) : ServerPath {

    /**
     * Needed for deserialization.
     */
    @Suppress("unused")
    internal constructor() : this(true, "", 0)

    fun toUrl(): String {
        return if (https) {
            if (port == HTTPS_DEFAULT_PORT) {
                "https://$host"
            } else {
                "https://$host:$port"
            }
        } else {
            if (port == HTTP_DEFAULT_PORT) {
                "http://$host"
            } else {
                "http://$host:$port"
            }
        }
    }

    fun isDefault(): Boolean {
        return this == DEFAULT
    }

    fun toDisplayName(): String {
        return if (https) {
            if (port == HTTPS_DEFAULT_PORT) {
                host
            } else {
                "$host:$port"
            }
        } else {
            if (port == HTTP_DEFAULT_PORT) {
                "http://$host"
            } else {
                "http://$host:$port"
            }
        }
    }

    override fun toString(): String {
        return toUrl()
    }

    fun isReferencedBy(remoteUrl: String): Boolean {
        return GitLabService.getProjectFromUrl(remoteUrl, this) != null
    }

    companion object {
        val DEFAULT = GitLabServerUrl(true, DEFAULT_HOST, HTTPS_DEFAULT_PORT)

        private val GITLAB_URL_REGEX: Pattern = Pattern.compile("""(https?)://([a-zA-Z\d-.]+)(?::(\d+))?/?""")

        @Throws(GitLabIllegalUrlException::class)
        fun parse(url: String): GitLabServerUrl {
            val matcher = GITLAB_URL_REGEX.matcher(url)
            if (matcher.matches()) {
                val https = matcher.group(1) == "https"

                val port: Int
                try {
                    port = matcher.group(3)?.let { Integer.valueOf(it) } ?: defaultPort(https)
                } catch (e: NumberFormatException) {
                    throw GitLabIllegalUrlException("Not a valid GitLab URL (illegal port ${matcher.group(3)}): $url")
                }

                return GitLabServerUrl(https, matcher.group(2), port)
            } else {
                throw GitLabIllegalUrlException("Not a valid GitLab URL: $url")
            }
        }

        private fun defaultPort(https: Boolean): Int = if (https) {
            HTTPS_DEFAULT_PORT
        } else {
            HTTP_DEFAULT_PORT
        }
    }
}

data class GitLabProjectCoord(val server: GitLabServerUrl, val projectPath: GitLabProjectPath) {
    @NlsSafe
    fun toDisplayName(): String {
        return if (server.isDefault()) {
            "${server.toDisplayName()}/${projectPath.toDisplayName()}"
        } else {
            projectPath.toDisplayName()
        }
    }

    fun toUrl(): String {
        return "${server.toUrl()}/${projectPath.path}"
    }

    override fun toString(): String {
        return toUrl()
    }
}

data class GitLabProjectPath(val path: String) {
    fun toDisplayName(): String {
        return path
    }

    fun asString(): String {
        return path
    }
}

enum class GitProtocol {
    HTTPS, SSH
}