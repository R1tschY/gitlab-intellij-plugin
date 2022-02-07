// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.model

import com.github.r1tschy.mergelab.exceptions.GitLabIllegalUrlException
import com.intellij.collaboration.api.ServerPath
import com.intellij.openapi.util.NlsSafe
import java.util.regex.Pattern
import kotlin.jvm.Throws

const val DEFAULT_HOST: String = "gitlab.com"
const val DEFAULT_URL: String = "https://$DEFAULT_HOST"
const val SERVICE_DISPLAY_NAME: String = "GitLab"

val GITLAB_URL_REGEX: Pattern = Pattern.compile("(https?)://([a-zA-Z0-9-.]+)(:\\d+)?/?")

data class MergeRequest(
    val conflicts: Boolean, val description: String?
)

data class GitLabInstanceCoord(val https: Boolean, val host: String, val port: Int) : ServerPath {
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

    companion object {
        @Throws(GitLabIllegalUrlException::class)
        fun parse(url: String): GitLabInstanceCoord {
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
                    throw GitLabIllegalUrlException("Not a valid GitLab URL")
                }

                return GitLabInstanceCoord(https, matcher.group(2), port)
            } else {
                throw GitLabIllegalUrlException("Not a valid GitLab URL")
            }
        }
    }
}

data class GitLabProjectCoord(val instance: GitLabInstanceCoord, val projectPath: GitLabProjectPath) {
    @NlsSafe
    fun toDisplayName(): String {
        return if (instance.isDefault()) {
            "${instance.toDisplayName()}/${projectPath.toDisplayName()}"
        } else {
            projectPath.toDisplayName()
        }
    }

    fun toUrl(): String {
        return "${instance.toUrl()}/${projectPath.path}"
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