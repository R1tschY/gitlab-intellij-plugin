package com.github.r1tschy.mergelab.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        //project.service<GitlabProjectManager>()
    }
}
