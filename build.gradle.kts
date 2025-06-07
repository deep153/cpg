val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val cpg_version = "10.3.2"  // Define CPG version

plugins {
    kotlin("jvm") version "2.1.0"  // Updated Kotlin version
    id("io.ktor.plugin") version "2.3.8"
    application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

application {
    mainClass.set("com.example.demo.DemoApplicationKt")
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Fraunhofer-AISEC/cpg")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker:$ktor_version")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")  // Updated Kotlin version
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")  // Updated Kotlin version
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.+")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    
    // Neo4j Java driver
    implementation("org.neo4j.driver:neo4j-java-driver:5.18.0")
    
    // CPG dependencies
    implementation("de.fraunhofer.aisec:cpg-core:$cpg_version")
    implementation("de.fraunhofer.aisec:cpg-language-llvm:$cpg_version")
    implementation("de.fraunhofer.aisec:cpg-neo4j:$cpg_version")
    
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check", "-Xcontext-receivers")
    }
} 
