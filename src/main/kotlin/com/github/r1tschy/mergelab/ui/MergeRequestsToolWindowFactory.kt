// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel


class MergeRequestsToolWindowFactory : ToolWindowFactory {

    data class MyData(var myString: String)

    private fun createMergeRequestContent(toolWindow: ToolWindow): DialogPanel {
        val myDataObject = MyData("")

        return panel {
            row("This is a row with a note") {}

            row {
                label("a string")
                textField().bindText(myDataObject::myString)
            }
        }
    }


    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(createMergeRequestContent(toolWindow), "", false)
        toolWindow.contentManager.addContent(content)
    }
}