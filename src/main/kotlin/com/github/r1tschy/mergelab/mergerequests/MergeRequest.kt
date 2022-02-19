package com.github.r1tschy.mergelab.mergerequests

data class MergeRequestId(private val id: String) {
    fun asString(): String = id
}

enum class MergeRequestState {
    OPEN,
    CLOSED,
    LOCKED,
    MERGED,
    OTHER
}


data class MergeRequest(
    val id: String,
    val iid: MergeRequestId,
    val title: String,
    val sourceBranch: String,
    val targetBranch: String,
    val state: MergeRequestState,
    val webUrl: String?)