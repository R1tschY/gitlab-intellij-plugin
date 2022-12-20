// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package de.richardliebscher.intellij.gitlab.accounts.ui

import de.richardliebscher.intellij.gitlab.accounts.Account
import kotlinx.coroutines.future.asCompletableFuture
import java.awt.Image
import java.util.concurrent.CompletableFuture
import javax.swing.Icon

internal class LoadingAvatarIconsProvider<A : Account>(private val detailsLoader: AccountsDetailsLoader<A, *>,
                                                       private val defaultAvatarIcon: Icon,
                                                       private val avatarUrlSupplier: (A) -> String?)
    : IconsProvider<A> {

    private val cachingDelegate = object : CachingCircleImageIconsProvider<Pair<A, String>>(defaultAvatarIcon) {
        override fun loadImageAsync(key: Pair<A, String>): CompletableFuture<Image?> =
            detailsLoader.loadAvatarAsync(key.first, key.second).asCompletableFuture()
    }


    override fun getIcon(key: A?, iconSize: Int): Icon {
        val account = key ?: return cachingDelegate.getIcon(null, iconSize)
        val uri = avatarUrlSupplier(key) ?: return cachingDelegate.getIcon(null, iconSize)
        return cachingDelegate.getIcon(account to uri, iconSize)
    }
}