// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.model

import com.intellij.openapi.project.Project

interface GitService {
    fun getRemotes(project: Project): List<String>
}