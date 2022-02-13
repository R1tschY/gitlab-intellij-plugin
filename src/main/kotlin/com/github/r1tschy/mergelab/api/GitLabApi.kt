package com.github.r1tschy.mergelab.api

import com.github.r1tschy.mergelab.accounts.GitLabUserApiService

interface GitLabApi: GitLabUserApiService, GitlabProtectedBranchesApiService