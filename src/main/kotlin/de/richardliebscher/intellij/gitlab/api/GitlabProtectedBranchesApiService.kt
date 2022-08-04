package de.richardliebscher.intellij.gitlab.api

import com.intellij.openapi.progress.ProgressIndicator
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import java.io.IOException

interface GitlabProtectedBranchesApiService {
    @Throws(IOException::class)
    fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String>
}