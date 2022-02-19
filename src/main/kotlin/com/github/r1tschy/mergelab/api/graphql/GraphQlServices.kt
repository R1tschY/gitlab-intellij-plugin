package com.github.r1tschy.mergelab.api.graphql

import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.api.*
import com.github.r1tschy.mergelab.api.graphql.queries.CurrentUser
import com.github.r1tschy.mergelab.api.graphql.queries.FindMergeRequestsUsingSourceBranch
import com.github.r1tschy.mergelab.api.graphql.queries.RepositoriesWithMembership
import com.github.r1tschy.mergelab.exceptions.UnauthorizedAccessException
import com.github.r1tschy.mergelab.mergerequests.MergeRequest
import com.github.r1tschy.mergelab.mergerequests.MergeRequestId
import com.github.r1tschy.mergelab.mergerequests.MergeRequestState
import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Image
import java.io.IOException
import javax.imageio.ImageIO
import com.github.r1tschy.mergelab.api.graphql.queries.enums.MergeRequestState as ApiMergeRequestState


class GraphQlServices(private val httpClient: HttpClient, private val token: GitlabAccessToken) : GitLabUserApiService,
    GitlabProjectsApiService, GitlabMergeRequestsApiService {

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
                avatarLocation = currentUser.avatarUrl
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

    override fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitlabRepositoryUrls> {
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
                    GitlabRepositoryUrls(
                        project.id,
                        project.name,
                        project.sshUrlToRepo,
                        project.httpUrlToRepo
                    )
                }
            }
            ?: emptyList()
    }

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
                        projectId = project.asString(),
                        sourceBranch = sourceBranch,
                        after = null
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