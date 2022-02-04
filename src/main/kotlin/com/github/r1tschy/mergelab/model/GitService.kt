package com.github.r1tschy.mergelab.model

import com.intellij.openapi.project.Project

interface GitService {
    fun getRemotes(project: Project): List<String>
}