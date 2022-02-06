package com.github.r1tschy.mergelab.accounts

data class GitLabAccessToken(private val token: String) {
    fun asString(): String = token

    override fun toString(): String {
        return "GitLabAccessToken(${token.substring(0, 3)}*****)"
    }
}