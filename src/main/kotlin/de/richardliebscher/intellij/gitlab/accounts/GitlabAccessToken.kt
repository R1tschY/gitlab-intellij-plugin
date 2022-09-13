// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.util.text.Strings
import com.intellij.util.Urls
import de.richardliebscher.intellij.gitlab.model.GitLabServerUrl


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
            "name" to "GitLab4Devs $productName plugin",
        )
    ).toString()
}