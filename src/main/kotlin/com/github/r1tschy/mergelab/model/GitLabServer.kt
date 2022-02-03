package com.github.r1tschy.mergelab.model

interface GitLabServer {
    fun getServerUrl(): String?
    fun getAccessToken(): String?
    fun getPreferredProtocol(): GitProtocol
}