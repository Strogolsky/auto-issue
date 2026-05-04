import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.io.FileInputStream
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.kotlin.serialization) // Kotlin Serialization (требуется для Koog)
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.ktlint) // Kotlin Linter
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Koog dependencies
    implementation(libs.koog.agents)
    implementation(libs.koog.agents.opentelemetry)
    compileOnly(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.core)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
    testImplementation(libs.mockk)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.apache5)

    runtimeOnly("org.slf4j:slf4j-simple:2.0.12")

    // IntelliJ Platform Gradle Plugin Dependencies Extension
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))

        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        bundledModules(providers.gradleProperty("platformBundledModules").map { it.split(',') })

        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin
intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }

        val changelog = project.changelog
        changeNotes =
            providers.gradleProperty("pluginVersion").map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels =
            providers.gradleProperty("pluginVersion")
                .map {
                    listOf(
                        it.substringAfter('-', "")
                            .substringBefore('.')
                            .ifEmpty { "default" },
                    )
                }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
    versionPrefix = ""
}

// Configure Gradle Kover Plugin
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    runIde {
        systemProperty("autoissue.llm.api-key", localProperties.getProperty("llm.api-key", ""))
        systemProperty("autoissue.jira.base-url", localProperties.getProperty("jira.base.url", ""))
        systemProperty("autoissue.jira.username", localProperties.getProperty("jira.username", ""))
        systemProperty("autoissue.jira.api-token", localProperties.getProperty("jira.api.token", ""))
        systemProperty("autoissue.jira.project-key", localProperties.getProperty("jira.project-key", ""))
        systemProperty("autoissue.langfuse.url", localProperties.getProperty("langfuse.url", ""))
        systemProperty("autoissue.langfuse.public-key", localProperties.getProperty("langfuse.public-key", ""))
        systemProperty("autoissue.langfuse.secret-key", localProperties.getProperty("langfuse.secret-key", ""))
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders +=
                    CommandLineArgumentProvider {
                        listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=",
                            "-Djb.consents.confirmation.enabled=false",
                        )
                    }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}

// Configure Gradle Ktlint Plugin
configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.0.1")
    verbose.set(true)
    outputToConsole.set(true)

    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
        force("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    }
}

listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
    it.configure {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
}
