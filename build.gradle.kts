@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.10")
    }
}

plugins {
    kotlin("jvm") version "1.9.0"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.10"
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

group = "kodash"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = java.sourceCompatibility.toString()
}

configure<PublishingExtension> {
    publications.create<MavenPublication>("maven") {
        from(components.getByName("kotlin"))
    }
}

tasks.dokkaHtml {
    pluginConfiguration<org.jetbrains.dokka.base.DokkaBase, org.jetbrains.dokka.base.DokkaBaseConfiguration> {
        footerMessage = "Copyright © StarWorld Team"
    }
}

tasks.withType<ProcessResources> {
    val resourceTargets = listOf("META-INF/kodash.properties")
    val replaceProperties = mapOf(
        Pair(
            "gradle",
            mapOf(
                Pair("gradle", gradle),
                Pair("project", project)
            )
        )
    )
    filesMatching(resourceTargets) {
        expand(replaceProperties)
    }
}
