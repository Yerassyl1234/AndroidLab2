pluginManagement {
    val localProperties = java.util.Properties().apply {
        val localPropertiesFile = file("local.properties")
        if (localPropertiesFile.exists()) {
            load(java.io.FileInputStream(localPropertiesFile))
        }
    }

    val githubUser = localProperties.getProperty("gpr.user")
    val githubToken = localProperties.getProperty("gpr.key")
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Yerassyl1234/AndroidLab2")
            credentials {
                username = githubUser
                password = githubToken
            }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val localProperties = java.util.Properties().apply {
        val localPropertiesFile = file("local.properties")
        if (localPropertiesFile.exists()) {
            load(java.io.FileInputStream(localPropertiesFile))
        }
    }

    val githubUser = localProperties.getProperty("gpr.user")
    val githubToken = localProperties.getProperty("gpr.key")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Yerassyl1234/AndroidLab2")
            credentials {
                username = githubUser
                password = githubToken
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Lab2"
include(":app")
include(":chatlibrary")