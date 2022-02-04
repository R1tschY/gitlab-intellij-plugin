package com.github.r1tschy.mergelab.git

import com.github.r1tschy.mergelab.model.GitService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.LocalFilePath
import git4idea.GitUtil
import git4idea.repo.GitRepository





class Git4IdeaService : GitService {
    override fun getRemotes(project: Project): List<String> {
        val repositoryManager = GitUtil.getRepositoryManager(project)
        repositoryManager.getRepositoryForFile(LocalFilePath("/", true))
        return listOf()
    }
}