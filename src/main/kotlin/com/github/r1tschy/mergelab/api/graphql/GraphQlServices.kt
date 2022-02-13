package com.github.r1tschy.mergelab.api.graphql

import com.github.r1tschy.mergelab.accounts.GitLabUserApiService
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.accounts.UserDetails
import com.github.r1tschy.mergelab.api.*
import com.github.r1tschy.mergelab.api.graphql.queries.CurrentUser
import com.github.r1tschy.mergelab.exceptions.UnauthorizedAccessException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Image
import java.io.IOException
import javax.imageio.ImageIO


class GraphQlServices(private val httpClient: HttpClient, private val token: GitlabAccessToken): GitLabUserApiService {

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
            return UserDetails(currentUser.username, currentUser.name, currentUser.avatarUrl)
        }
    }

    @Throws(IOException::class)
    @RequiresBackgroundThread
    override fun getAvatar(processIndicator: ProgressIndicator, url: String): Image? {
        // TODO: use token when URL matches?
        return httpClient.execute(object : HttpRequest<Image?> {
            override val url = url

            override fun readContent(response: HttpResponse): Image? {
                return response.readBody { inputStream ->
                    ImageIO.read(inputStream)
                }
            }
        }, processIndicator)
    }
}