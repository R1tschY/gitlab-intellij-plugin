package de.richardliebscher.intellij.gitlab.api.graphql

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.api.*
import de.richardliebscher.intellij.gitlab.api.graphql.queries.CurrentUser
import de.richardliebscher.intellij.gitlab.api.graphql.queries.FindMergeRequestsUsingSourceBranch
import de.richardliebscher.intellij.gitlab.api.graphql.queries.RepositoriesWithMembership
import de.richardliebscher.intellij.gitlab.api.graphql.queries.SearchProjects
import de.richardliebscher.intellij.gitlab.api.graphql.queries.searchprojects.Project
import de.richardliebscher.intellij.gitlab.exceptions.UnauthorizedAccessException
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequest
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequestId
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequestState
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import java.awt.Image
import java.io.IOException
import javax.imageio.ImageIO
import de.richardliebscher.intellij.gitlab.api.graphql.queries.enums.MergeRequestState as ApiMergeRequestState

const val DEFAULT_PAGINATION_SIZE = 100
const val MAX_LOADING_ELEMENTS = 500


data class PageInfo(val endCursor: String?, val hasNextPage: Boolean) {
    companion object {
        fun empty(): PageInfo {
            return PageInfo(null, false)
        }

        fun firstPage(): PageInfo {
            return PageInfo(null, true)
        }
    }
}

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
        sort: String?,
        processIndicator: ProgressIndicator
    ): List<GitLabRepositoryUrls> {
        return loadAll(
            { l: MutableList<Project>, v -> l.addAllIfNotNull(v.projects?.nodes) },
            { projects?.run { PageInfo(pageInfo.endCursor, pageInfo.hasNextPage) } ?: PageInfo.empty() }
        ) { after, first ->
            httpClient
                .query(
                    SearchProjects(
                        SearchProjects.Variables(
                            q = query,
                            membership = membership,
                            sort = sort,
                            after = after,
                            first = first
                        )
                    ),
                    processIndicator, BearerAuthorization(token)
                )
                .check()
        }.map {
            GitLabRepositoryUrls(
                id = it.fullPath,
                name = it.name,
                sshUrl = it.sshUrlToRepo,
                httpsUrl = it.httpUrlToRepo
            )
        }
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

fun <T> MutableList<T>.addAllIfNotNull(elements: Collection<T?>?) {
    elements?.forEach { e -> e?.let(::add) }
}

private fun <T, R> loadAll(
    accu: (MutableList<R>, T) -> Unit,
    pageInfo: T.() -> PageInfo,
    query: (after: String?, first: Int) -> T
): List<R> {
    var lastPage = PageInfo.firstPage()
    val ret = mutableListOf<R>()

    do {
        val value = query.invoke(lastPage.endCursor, DEFAULT_PAGINATION_SIZE)
        lastPage = pageInfo.invoke(value)
        accu.invoke(ret, value)
    } while (lastPage.hasNextPage && lastPage.endCursor != null && ret.size < MAX_LOADING_ELEMENTS)

    return ret
}