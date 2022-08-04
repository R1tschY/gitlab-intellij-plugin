package de.richardliebscher.intellij.gitlab.model

//import git4idea.repo.GitRemote
//import org.junit.jupiter.api.Test
//import kotlin.test.assertEquals
//import org.junit.jupiter.params.ParameterizedTest
//import org.junit.jupiter.params.provider.ValueSource
//import java.net.URL
//import kotlin.test.assertNull

class GitLabServiceTest {
//
//    @Test
//    fun `Git url should be detected`() {
//        val remote = "git@gitlab.com:USER/REPO.git"
//
//        val gitLabProject = GitLabService().getMatchingRemoteFromUrl(
//            GitRemote.ORIGIN, remote, URL("https://gitlab.com"))
//
//        assertEquals(GitLabRemote(GitRemote.ORIGIN, GitLabProjectPath("USER/REPO")), gitLabProject)
//    }
//
//    @Test
//    fun `HTTPS url should be detected`() {
//        val remote = "https://gitlab.com/USER/REPO.git"
//
//        val gitLabProject = GitLabService().getMatchingRemoteFromUrl(
//            GitRemote.ORIGIN, remote, URL("https://gitlab.com"))
//
//        assertEquals(GitLabRemote(GitRemote.ORIGIN, GitLabProjectPath("USER/REPO")), gitLabProject)
//    }
//
//    @Test
//    fun `HTTP url should be detected`() {
//        val remote = "http://gitlab.com/USER/REPO.git"
//
//        val gitLabProject = GitLabService().getMatchingRemoteFromUrl(
//            GitRemote.ORIGIN, remote, URL("https://gitlab.com"))
//
//        assertEquals(GitLabRemote(GitRemote.ORIGIN, GitLabProjectPath("USER/REPO")), gitLabProject)
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = [
//        "PROTOCOL://gitlab.com/USER/REPO.git",
//        "https://other.gitlab.org/USER/REPO.git",
//        "git@other.gitlab.org:USER/REPO.git",
//        "https://gitlab.org/USER/REPOSITORY"
//    ])
//    fun `Service should ignore other servers`(url: String) {
//        val gitLabProject = GitLabService().getMatchingRemoteFromUrl(
//            GitRemote.ORIGIN, url, URL("https://gitlab.com"))
//
//        assertNull(gitLabProject)
//    }
}