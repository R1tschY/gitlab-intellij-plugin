// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.util.text.Strings
import com.intellij.util.Urls


val REQUIRED_SCOPES = listOf("read_api")


data class GitlabAccessToken(private val token: String) {
    fun asString(): String = token

    override fun toString(): String {
        return "GitlabAccessToken(${token.substring(0, 3)}*****)"
    }
}


fun buildNewTokenUrl(server: GitLabServerUrl): String {
    val productName = ApplicationNamesInfo.getInstance().fullProductName

    return Urls.newUrl(
        scheme = if (server.https) { "https" } else { "http" },
        authority = "${server.host}:${server.port}",
        path = "/-/profile/personal_access_tokens",
        parameters = mapOf(
            "scopes" to Strings.join(REQUIRED_SCOPES, ","),
            "name" to "MergeLab $productName plugin",
        )
    ).toString()
}