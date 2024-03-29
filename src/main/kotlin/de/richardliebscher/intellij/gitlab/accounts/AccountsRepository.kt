// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts

/**
 * In most cases should be an instance of [com.intellij.openapi.components.PersistentStateComponent]
 */
interface AccountsRepository<A: Account> {
    var accounts: Set<A>
}