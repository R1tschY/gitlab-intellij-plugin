package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ImageLoader
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import de.richardliebscher.intellij.gitlab.api.GitLabApi
import de.richardliebscher.intellij.gitlab.utils.CompletableFutureUtil
import de.richardliebscher.intellij.gitlab.utils.CompletableFutureUtil.submitIOTask
import org.jetbrains.annotations.CalledInAny
import java.awt.Image
import java.util.concurrent.CompletableFuture

@Service
class GitLabAvatarService {

    @CalledInAny
    fun loadAvatar(api: GitLabApi, url: String, indicator: ProgressIndicator): CompletableFuture<Image?> {
        return ProgressManager.getInstance().submitIOTask(indicator) {
            loadAvatarSync(api, url, indicator)
        }
    }

    @RequiresBackgroundThread
    fun loadAvatarSync(api: GitLabApi, url: String, indicator: ProgressIndicator): Image? {
        // TODO: use Cache
        return try {
            val avatar = api.getAvatar(indicator, url)
            if (avatar != null && avatar.getWidth(null) >= MAX_ICON_SIZE && avatar.getHeight(null) >= MAX_ICON_SIZE) {
                ImageLoader.scaleImage(avatar, MAX_ICON_SIZE)
            } else {
                avatar
            }
        } catch (e: Exception) {
            if (!CompletableFutureUtil.isCancellation(e)) {
                LOG.warn("Failed to load image from $url", e)
            }
            null
        }
    }

    companion object {
        private val LOG = thisLogger()

        private const val MAX_ICON_SIZE = 40 * 6
    }
}