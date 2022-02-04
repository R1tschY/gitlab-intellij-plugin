package com.github.r1tschy.mergelab.model

data class MergeRequest(
    val conflicts: Boolean,
    val description: String?)

data class GitLabProjectId(val id: String)

enum class GitProtocol {
    HTTPS,
    SSH
}