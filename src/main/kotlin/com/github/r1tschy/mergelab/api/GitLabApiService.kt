package com.github.r1tschy.mergelab.api

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.api.graphql.GraphQlServices
import com.github.r1tschy.mergelab.api.intellij.IntellijHttpClient
import com.github.r1tschy.mergelab.api.restV4.JacksonJsonSerializer
import com.github.r1tschy.mergelab.api.restV4.RestApiV4Services
import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import java.awt.Image

class GitLabApiImpl(private val graphQl: GraphQlServices, private val restApi: RestApiV4Services): GitLabApi {
    override fun getUserDetails(processIndicator: ProgressIndicator): UserDetails {
        return graphQl.getUserDetails(processIndicator)
    }

    override fun getAvatar(processIndicator: ProgressIndicator, url: String): Image? {
        return graphQl.getAvatar(processIndicator, url)
    }

    override fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String> {
        return restApi.getProtectedBranches(project, processIndicator)
    }

    override fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitlabRepositoryUrls> {
        return graphQl.getRepositoriesWithMembership(processIndicator)
    }
}

@Service
class GitLabApiService {

    private val serializer: GraphQLClientSerializer = GraphQLClientJacksonSerializer()
    private val restSerializer: JsonSerializer = JacksonJsonSerializer()

    private val authService: GitLabAuthService = service()

    fun apiFor(account: GitLabAccount): GitLabApi? {
        return authService.getToken(account)?.let { apiFor(account.server, it) }
    }

    fun apiFor(server: GitLabServerUrl, token: GitlabAccessToken): GitLabApi {
        val httpClient = IntellijHttpClient(server.toUrl(), serializer, restSerializer)
        val graphQl = GraphQlServices(httpClient, token)
        val restApi = RestApiV4Services(httpClient, token)
        return GitLabApiImpl(graphQl, restApi)
    }
}