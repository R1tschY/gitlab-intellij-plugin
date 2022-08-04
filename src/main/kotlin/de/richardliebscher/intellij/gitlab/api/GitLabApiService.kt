package de.richardliebscher.intellij.gitlab.api

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.serializer.GraphQLClientSerializer
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresEdt
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitLabAuthService
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.api.graphql.GraphQlServices
import de.richardliebscher.intellij.gitlab.api.intellij.IntellijHttpClient
import de.richardliebscher.intellij.gitlab.api.restV4.JacksonJsonSerializer
import de.richardliebscher.intellij.gitlab.api.restV4.RestApiV4Services
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequest
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import de.richardliebscher.intellij.gitlab.model.GitLabServerUrl
import org.jetbrains.annotations.CalledInAny
import java.awt.Image
import java.io.IOException

class GitLabApiImpl(private val graphQl: GraphQlServices, private val restApi: RestApiV4Services) : GitLabApi {
    @Throws(IOException::class)
    override fun getUserDetails(processIndicator: ProgressIndicator): UserDetails {
        return graphQl.getUserDetails(processIndicator)
    }

    @Throws(IOException::class)
    override fun getAvatar(processIndicator: ProgressIndicator, location: String): Image? {
        return graphQl.getAvatar(processIndicator, location)
    }

    @Throws(IOException::class)
    override fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String> {
        return restApi.getProtectedBranches(project, processIndicator)
    }

    @Throws(IOException::class)
    override fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitlabRepositoryUrls> {
        return graphQl.getRepositoriesWithMembership(processIndicator)
    }

    @Throws(IOException::class)
    override fun search(
        query: String?,
        membership: Boolean,
        sort: String,
        processIndicator: ProgressIndicator
    ): List<GitlabRepositoryUrls> {
        return graphQl.search(query, membership, sort, processIndicator)
    }

    @Throws(IOException::class)
    override fun findMergeRequestsUsingSourceBranch(
        project: GitLabProjectPath,
        sourceBranch: String,
        processIndicator: ProgressIndicator
    ): List<MergeRequest> {
        return graphQl.findMergeRequestsUsingSourceBranch(project, sourceBranch, processIndicator)
    }
}

@Service
class GitLabApiService {

    private val serializer: GraphQLClientSerializer = GraphQLClientJacksonSerializer()
    private val restSerializer: JsonSerializer = JacksonJsonSerializer()

    private val authService: GitLabAuthService = service()

    @CalledInAny
    fun apiFor(account: GitLabAccount): GitLabApi? {
        return authService.getToken(account)?.let { apiFor(account.server, it) }
    }

    @CalledInAny
    fun apiFor(server: GitLabServerUrl, token: GitlabAccessToken): GitLabApi {
        val httpClient = IntellijHttpClient(server.toUrl(), serializer, restSerializer)
        val graphQl = GraphQlServices(httpClient, token)
        val restApi = RestApiV4Services(httpClient, token)
        return GitLabApiImpl(graphQl, restApi)
    }

    @RequiresEdt
    fun apiForRemoteUrl(remoteUrl: String): GitLabApi? {
        return authService.findAccountByRemoteUrl(remoteUrl)?.let { apiFor(it) }
    }

    @RequiresEdt
    fun apiFor(serverUrl: GitLabServerUrl): GitLabApi? {
        return authService.findAccountByServerUrl(serverUrl)?.let { apiFor(it) }
    }
}