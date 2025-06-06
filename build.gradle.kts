// Removed invalid internal import
// import com.sun.javafx.scene.CameraHelper.project

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("maven-publish")
    id("com.modrinth.minotaur") version "2.+"
}

val pversion: String by gradle.extra
val pluginType: String by gradle.extra
val pluginVersions: List<String> by gradle.extra
val pluginLoaders: List<String> by gradle.extra

group = "dev.consti"
version = pversion

allprojects {
    repositories {
        mavenCentral()
    }
}


repositories {
    mavenCentral()
    maven { url = uri("https://repo.william278.net/releases/") }
    maven { url = uri("https://repo.extendedclip.com/releases/") }
}

dependencies {
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {


    shadowJar {

        dependsOn(":paper:shadowJar")
        manifest { attributes["paperweight-mappings-namespace"] = "spigot" }

        relocate("dev.jorel.commandapi", "dev.consti.commandbridge.commandapi")
        relocate("org.bstats", "dev.consti.commandbridge.bstats")

        from(project(":paper").sourceSets.main.get().output)
        from(project(":velocity").sourceSets.main.get().output)

        configurations = listOf(project.configurations.runtimeClasspath.get())
        mergeServiceFiles()
    }

    val copyToPaperPlugins by registering(Copy::class) {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val paperPluginDir = if (isWindows)
            "C:/Users/Florian/workspace/spoutmc/testservers/data/spoutlobby/plugins"
        else
            "/mnt/Storage/Server-TEST/CommandBridge/Paper/plugins"
        into(paperPluginDir)
    }

    val copyToVelocityPlugins by registering(Copy::class) {
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val velocityPluginDir = if (isWindows)
            "C:/Users/Florian/workspace/spoutmc/testservers/data/spoutproxy/plugins"
        else
            "/mnt/Storage/Server-TEST/CommandBridge/Velocity/plugins"
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        into(velocityPluginDir)
    }

    register("dev") {
        dependsOn(copyToPaperPlugins, copyToVelocityPlugins, "restartDocker")
    }
}

tasks.register("restartDocker") {
    doLast {
        exec {
            commandLine("docker", "restart", "lobby")
        }
        exec {
            commandLine("docker", "restart", "skyblock")
        }
        exec {

            commandLine("docker", "restart", "spoutproxy")
        }
    }
}


afterEvaluate {
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("wIuI4ru2")
        versionName.set("CommandBridge $pversion")
        changelog.set(rootProject.file("CHANGELOG.md").readText())
        versionNumber.set(pversion)
        versionType.set(pluginType)
        uploadFile.set(tasks.shadowJar)
        gameVersions.set(pluginVersions)
        loaders.set(pluginLoaders)
        debugMode.set(false)
    }
}
