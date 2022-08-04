package de.richardliebscher.intellij.gitlab.api

interface GitLabApi : GitLabUserApiService, GitlabProtectedBranchesApiService, GitlabProjectsApiService,
    GitlabMergeRequestsApiService