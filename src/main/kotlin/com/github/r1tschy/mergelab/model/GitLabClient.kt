package com.github.r1tschy.mergelab.model

interface GitLabClient {
    fun echo(text: String): String
}