// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

data class GitLabAccessToken(private val token: String) {
    fun asString(): String = token

    override fun toString(): String {
        return "GitLabAccessToken(${token.substring(0, 3)}*****)"
    }
}