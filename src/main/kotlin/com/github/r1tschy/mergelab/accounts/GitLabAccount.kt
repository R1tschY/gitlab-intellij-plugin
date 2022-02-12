// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.DEFAULT_HOST
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.collaboration.auth.ServerAccount
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient

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
    override fun toString(): String = "$server/$name"
}