import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

val useJackson: Boolean = properties("useJacksonSerializer") == "true"

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    // Gradle IntelliJ Plugin
    id("org.jetbrains.intellij") version "1.11.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.0.0"
    // Gradle Qodana Plugin
    id("org.jetbrains.qodana") version "0.1.13"
    // Gradle Kover Plugin
    id("org.jetbrains.kotlinx.kover") version "0.6.1"

    // kotlinx.serialization
    kotlin("plugin.serialization") version "1.7.22"

    // GraphQL query objects generator
    id("com.expediagroup.graphql") version "6.3.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion") + "+" + properties("pluginSinceBuild")

// Configure project's dependencies
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.expediagroup", "graphql-kotlin-client", "5.3.2") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    if (useJackson) {
        implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.9.8") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(group = "org.jetbrains.kotlinx")
        }
        implementation("com.expediagroup", "graphql-kotlin-client-jackson", "5.3.2") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(group = "org.jetbrains.kotlinx")
        }
    } else {
        implementation("com.expediagroup", "graphql-kotlin-client-serialization", "5.3.2") {
            exclude(group = "org.jetbrains.kotlin")
            exclude(group = "org.jetbrains.kotlinx")
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

// Set the JVM language level used to compile sources and generate files - Java 17 is required since 2022.3
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
qodana {
    cachePath.set(file(".qodana").canonicalPath)
    reportPath.set(file("build/reports/inspections").canonicalPath)
    saveReport.set(true)
    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover.xmlReport {
    onCheck.set(true)
}

graphql {
    client {
        endpoint = "https://gitlab.com/api/graphql"
        packageName = "de.richardliebscher.intellij.gitlab.api.graphql.queries"
        serializer = if (useJackson) { GraphQLSerializer.JACKSON } else { GraphQLSerializer.KOTLINX }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.languageVersion = "1.7"
        kotlinOptions.apiVersion = "1.5"
        kotlinOptions.jvmTarget = "17"
    }

    test {
        useJUnitPlatform()
    }

    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    graphqlGenerateTestClient.configure { this.enabled = false }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            file("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").let { markdownToHTML(it) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion")) ?: getUnreleased(), Changelog.OutputType.HTML)
            }
        })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }
}
