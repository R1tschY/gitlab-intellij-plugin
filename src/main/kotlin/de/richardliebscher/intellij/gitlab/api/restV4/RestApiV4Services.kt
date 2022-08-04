package de.richardliebscher.intellij.gitlab.api.restV4

import com.intellij.openapi.progress.ProgressIndicator
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.api.*
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RestApiAuthorization(private val token: GitlabAccessToken): HttpRequestCustomizer {
    override fun customize(request: HttpRequestBuilder) {
        request.addHeader("PRIVATE-TOKEN", token.asString())
    }
}

data class ProtectedBranch(
    val id: Int, val name: String, val allow_force_push: Boolean, val code_owner_approval_required: Boolean)

class RestApiV4Services(private val httpClient: HttpClient, private val token: GitlabAccessToken) : GitlabProtectedBranchesApiService {
    override fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String> {
        // TODO: use token when URL matches?
        val protectedBranches = httpClient.execute(object : JsonRequest<List<ProtectedBranch>> {
            override val location = getProjectApiPath(project) + "/protected_branches"

            override fun deserialize(rawResponse: String, serializer: JsonSerializer): List<ProtectedBranch> {
                return serializer.deserializeList(rawResponse, ProtectedBranch::class)
            }
        }, processIndicator, RestApiAuthorization(token))

        return protectedBranches.filter { !it.allow_force_push }.map { it.name }
    }

    private fun getProjectApiPath(project: GitLabProjectPath): String {
        return "/api/v4/projects/" + URLEncoder.encode(project.path, StandardCharsets.UTF_8)
    }
}