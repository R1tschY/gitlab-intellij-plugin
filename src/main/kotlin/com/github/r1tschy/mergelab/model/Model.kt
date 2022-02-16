// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.model

import com.github.r1tschy.mergelab.exceptions.GitLabIllegalUrlException
import com.intellij.collaboration.api.ServerPath
import com.intellij.openapi.util.NlsSafe
import java.util.regex.Pattern

const val DEFAULT_HOST: String = "gitlab.com"
const val DEFAULT_URL: String = "https://$DEFAULT_HOST"
val DEFAULT_SERVER_URL: GitLabServerUrl = GitLabServerUrl(true, DEFAULT_HOST, 443)

const val SERVICE_DISPLAY_NAME: String = "GitLab"

val GITLAB_URL_REGEX: Pattern = Pattern.compile("(https?)://([a-zA-Z0-9-.]+)(?::(\\d+))?/?")


data class GitLabServerUrl(val https: Boolean, val host: String, val port: Int) : ServerPath {
    fun toUrl(): String {
        if (https) {
            return "https://$host:$port"
        } else {
            return "http://$host:$port"
        }
    }

    fun isDefault(): Boolean {
        return https && host == DEFAULT_HOST && port == 443
    }

    fun toDisplayName(): String {
        if (https) {
            if (port == 443) {
                return host
            } else {
                return "$host:$port"
            }
        } else {
            if (port == 80) {
                return "http://$host"
            } else {
                return "http://$host:$port"
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
        @Throws(GitLabIllegalUrlException::class)
        fun parse(url: String): GitLabServerUrl {
            val matcher = GITLAB_URL_REGEX.matcher(url)
            if (matcher.matches()) {
                val https = matcher.group(1) == "https"

                val port: Int
                try {
                    port = matcher.group(3)?.let { Integer.valueOf(it) } ?: (if (https) {
                        443
                    } else {
                        80
                    })
                } catch (e: NumberFormatException) {
                    throw GitLabIllegalUrlException("Not a valid GitLab URL (illegal port ${matcher.group(3)}): $url")
                }

                return GitLabServerUrl(https, matcher.group(2), port)
            } else {
                throw GitLabIllegalUrlException("Not a valid GitLab URL: $url")
            }
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
}

data class GitLabProjectPath(val path: String) {
    fun toDisplayName(): String {
        return path
    }
}

enum class GitProtocol {
    HTTPS, SSH
}