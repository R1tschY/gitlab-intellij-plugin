package de.richardliebscher.intellij.gitlab.exceptions

class UnauthorizedAccessException : GitLabException("Unauthorized access to GitLab. Please check your access token.")