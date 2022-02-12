package com.github.r1tschy.mergelab.api

import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.api.graphql.GraphQlServices
import com.github.r1tschy.mergelab.api.intellij.IntellijHttpClient
import com.github.r1tschy.mergelab.exceptions.UnauthorizedAccessException
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class GitLabApiService {

    private val serializer: GraphQLClientSerializer = GraphQLClientKotlinxSerializer()

    private val authService: GitLabAuthService = service()

    @Throws(UnauthorizedAccessException::class)
    fun apiFor(account: GitLabAccount): GitLabApi {
        val token = authService.getToken(account) ?: throw UnauthorizedAccessException()
        return GraphQlServices(IntellijHttpClient(account.server.toUrl(), serializer), token)
    }

    fun apiFor(server: GitLabServerUrl, token: GitlabAccessToken): GitLabApi {
        return GraphQlServices(IntellijHttpClient(server.toUrl(), serializer), token)
    }
}