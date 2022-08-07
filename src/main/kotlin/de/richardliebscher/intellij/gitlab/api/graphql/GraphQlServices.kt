package de.richardliebscher.intellij.gitlab.api.graphql

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.api.*
import de.richardliebscher.intellij.gitlab.api.graphql.queries.CurrentUser
import de.richardliebscher.intellij.gitlab.api.graphql.queries.FindMergeRequestsUsingSourceBranch
import de.richardliebscher.intellij.gitlab.api.graphql.queries.RepositoriesWithMembership
import de.richardliebscher.intellij.gitlab.api.graphql.queries.SearchProjects
import de.richardliebscher.intellij.gitlab.exceptions.UnauthorizedAccessException
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequest
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequestId
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequestState
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import java.awt.Image
import java.io.IOException
import javax.imageio.ImageIO
import de.richardliebscher.intellij.gitlab.api.graphql.queries.enums.MergeRequestState as ApiMergeRequestState


class GraphQlServices(private val httpClient: HttpClient, private val token: GitlabAccessToken) : GitLabUserApiService,
    GitlabProjectsApiService, GitlabMergeRequestsApiService {

    @Throws(IOException::class)
    @RequiresBackgroundThread
    override fun getUserDetails(
        processIndicator: ProgressIndicator
    ): UserDetails {
        val currentUser = httpClient
            .query(CurrentUser(), processIndicator, BearerAuthorization(token))
            .check()
            .currentUser
        if (currentUser == null) {
            throw UnauthorizedAccessException()
        } else {
            return UserDetails(
                username = currentUser.username,
                name = currentUser.name,
                avatarUrl = currentUser.avatarUrl
            )
        }
    }

    @Throws(IOException::class)
    @RequiresBackgroundThread
    override fun getAvatar(processIndicator: ProgressIndicator, location: String): Image? {
        // TODO: use token when URL matches?
        return httpClient.execute(object : HttpRequest<Image?> {
            override val location = location

            override fun readContent(response: HttpResponse): Image? {
                return response.readBody { inputStream ->
                    ImageIO.read(inputStream)
                }
            }
        }, processIndicator)
    }

    @Throws(IOException::class)
    override fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitLabRepositoryUrls> {
        // TODO: pagination
        return httpClient
            .query(
                RepositoriesWithMembership(
                    RepositoriesWithMembership.Variables(after = null)
                ), processIndicator, BearerAuthorization(token)
            )
            .check()
            .currentUser
            ?.projectMemberships
            ?.nodes
            ?.mapNotNull {
                it?.project?.let { project ->
                    GitLabRepositoryUrls(
                        project.id,
                        project.name,
                        project.sshUrlToRepo,
                        project.httpUrlToRepo
                    )
                }
            }
            ?: emptyList()
    }

    @Throws(IOException::class)
    override fun search(
        query: String?,
        membership: Boolean,
        sort: String,
        processIndicator: ProgressIndicator
    ): List<GitLabRepositoryUrls> {
        return httpClient
            .query(
                SearchProjects(
                    SearchProjects.Variables(q = query, membership = membership, sort = sort, after = null, first = 20)
                ),
                processIndicator, BearerAuthorization(token)
            )
            .check()
            .projects
            ?.nodes
            ?.mapNotNull {
                it?.let { project ->
                    GitLabRepositoryUrls(id = project.fullPath, name = project.name, sshUrl = null, httpsUrl = null)
                }
            }
            ?: emptyList()
    }

    @Throws(IOException::class)
    override fun findMergeRequestsUsingSourceBranch(
        project: GitLabProjectPath,
        sourceBranch: String,
        processIndicator: ProgressIndicator
    ): List<MergeRequest> {
        // TODO: pagination
        return httpClient
            .query(
                FindMergeRequestsUsingSourceBranch(
                    FindMergeRequestsUsingSourceBranch.Variables(
                        projectId = project.asString(), sourceBranch = sourceBranch, after = null
                    )
                ),
                processIndicator, BearerAuthorization(token)
            )
            .check()
            .project
            ?.mergeRequests
            ?.nodes
            ?.mapNotNull {
                it?.let { mr ->
                    MergeRequest(
                        id = mr.id, iid = MergeRequestId(mr.iid), title = mr.title, sourceBranch = mr.sourceBranch,
                        targetBranch = mr.targetBranch, state = mapState(mr.state), webUrl = mr.webUrl
                    )
                }
            }
            ?: emptyList()
    }

    private fun mapState(state: ApiMergeRequestState): MergeRequestState {
        return when (state) {
            ApiMergeRequestState.OPENED -> MergeRequestState.OPEN
            ApiMergeRequestState.MERGED -> MergeRequestState.MERGED
            ApiMergeRequestState.CLOSED -> MergeRequestState.CLOSED
            ApiMergeRequestState.LOCKED -> MergeRequestState.LOCKED
            else -> MergeRequestState.OTHER
        }
    }
}