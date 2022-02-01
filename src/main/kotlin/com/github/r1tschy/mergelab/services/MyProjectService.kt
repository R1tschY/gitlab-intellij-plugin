package com.github.r1tschy.mergelab.services

import com.intellij.openapi.project.Project
import com.github.r1tschy.mergelab.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
