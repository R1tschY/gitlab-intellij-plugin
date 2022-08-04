// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.collaboration.auth.ServerAccount
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import de.richardliebscher.intellij.gitlab.model.DEFAULT_HOST
import de.richardliebscher.intellij.gitlab.model.DEFAULT_SERVER_URL
import de.richardliebscher.intellij.gitlab.model.GitLabServerUrl

@Tag("account")
class GitLabAccount(
    @set:Transient
    @NlsSafe
    @Attribute("name")
    override var name: String = "",

    @Property(style = Property.Style.ATTRIBUTE, surroundWithTag = false)
    override val server: GitLabServerUrl = GitLabServerUrl(true, DEFAULT_HOST, 443),

    @Attribute("id")
    override val id: String = generateId()
) : ServerAccount() {
    override fun toString(): String {
        return if (server == DEFAULT_SERVER_URL) {
            name
        } else {
            "$server/$name"
        }
    }
}