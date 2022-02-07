// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.util.text.Strings
import io.ktor.http.*


val REQUIRED_SCOPES = listOf("read_api")


data class GitLabAccessToken(private val token: String) {
    fun asString(): String = token

    override fun toString(): String {
        return "GitLabAccessToken(${token.substring(0, 3)}*****)"
    }
}


fun buildNewTokenUrl(server: GitLabInstanceCoord): String {
    val productName = ApplicationNamesInfo.getInstance().fullProductName

    return URLBuilder(
        protocol = if (server.https) { URLProtocol.HTTPS } else { URLProtocol.HTTP },
        encodedPath = "/-/profile/personal_access_tokens",
        host = server.host,
        port = server.port,
    ).apply {
        parameters.append("scopes", Strings.join(REQUIRED_SCOPES, ","))
        parameters.append("name", "MergeLab $productName plugin")
    }.buildString()
}