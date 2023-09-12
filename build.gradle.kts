plugins {
    kotlin("jvm") version "1.9.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "space.outbreak"
version = "1.0-SNAPSHOT"
description = rootProject.name

bukkit {
    version = rootProject.version.toString()
    name = rootProject.name
    main = "space.outbreak.hatfactory.HatFactoryPlugin"
    apiVersion = "1.20"
    authors = listOf("OUTBREAK")
}

apply(plugin = "kotlin")

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade:9.0.3")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("org.apache.commons:commons-text:1.10.0")
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    relocate("dev.jorel.commandapi", "${rootProject.group}.${rootProject.name.toLowerCase()}.commandapi")

    manifest {
        attributes("Implementation-Version" to rootProject.version)
    }

    archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
//    destinationDirectory.set(file("D:\\test_server_light\\plugins\\"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}