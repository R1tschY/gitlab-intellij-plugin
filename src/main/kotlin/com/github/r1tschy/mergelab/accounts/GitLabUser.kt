package com.github.r1tschy.mergelab.accounts

import com.intellij.collaboration.auth.AccountDetails

data class GitLabUser(override val name: String) : AccountDetails