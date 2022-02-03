package com.github.r1tschy.mergelab.graphql

import com.github.r1tschy.mergelab.model.GitLabClient
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.github.r1tschy.mergelab.gitlabapi.Echo
import kotlinx.coroutines.runBlocking
import java.net.URL

class GitLabGraphQLClient : GitLabClient {
    override fun echo(text: String): String {
        GraphQLKtorClient(URL("http://gitlab.com/api/graphql")).use { client ->
            var result: String
            runBlocking {
                result = client.execute(Echo(Echo.Variables(text))).data!!.echo
            }
            return result
        }
    }
}