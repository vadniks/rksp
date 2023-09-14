import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "me.admin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("commons-io:commons-io:2.13.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.7")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
}

