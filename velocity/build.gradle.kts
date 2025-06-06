//import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.tasks.Copy


plugins {
    id("java")
}

val pversion: String by gradle.extra

group = "dev.consti"
version = pversion

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.william278.net/releases/") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(localGroovy())
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    compileOnly("net.william278:papiproxybridge:1.7.2")
    implementation("org.bstats:bstats-velocity:3.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":foundationlib"))
    implementation("io.netty:netty-codec-http:4.1.100.Final")
    implementation("org.json:json:20240303")


}

/*tasks.register("modifyVelocityPluginJson") {
    dependsOn("classes") // Ensure the compiled class is available
    //dependsOn("compileKotlin")
    doLast {
        val jsonFile = layout.buildDirectory.file("classes/java/main/velocity-plugin.json").get().asFile
        if (jsonFile.exists()) {
            println("Found velocity-plugin.json")
            val runner = javaClass.classLoader.loadClass("dev.consti.commandbridge.velocity.util")
            val method = runner.getMethod("updateJsonVersion", Path::class.java, String::class.java)
            method.invoke(null, jsonFile.toPath(), pversion)
            println("velocity-plugin.json updated successfully with version $pversion")
        } else {
            println("velocity-plugin.json not found")
        }
    }
}*/

tasks.register("modifyVelocityPluginJson") {
    doLast {
        val jsonFile = layout.buildDirectory.file("classes/java/main/velocity-plugin.json").get().asFile
        if (jsonFile.exists()) {
            println("Found velocity-plugin.json")

            val slurper = JsonSlurper()
            val json = slurper.parse(jsonFile) as Map<String, Any>
            val mutableJson = json.toMutableMap()
            val version = project.findProperty("pversion")?.toString() ?: "1.0.0"
            mutableJson["version"] = version

            jsonFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mutableJson)))
            println("velocity-plugin.json updated successfully with version $version")
        } else {
            println("velocity-plugin.json not found")
        }
    }
}


tasks.register("generatePluginProperties") {
    doLast {
        val propertiesFile = layout.buildDirectory.file("resources/main/plugin.properties").get().asFile
        propertiesFile.parentFile.mkdirs()
        propertiesFile.writeText("plugin.version=$pversion\n")
        println("plugin.properties generated")
    }
}

tasks.named("processResources") {
    dependsOn("generatePluginProperties")
    finalizedBy("modifyVelocityPluginJson")
}
