plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.compose") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

group = "org.gelov"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Exposed ORM + JDBC
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")

    // MySQL Treiber
    implementation("mysql:mysql-connector-java:8.0.33")

    implementation("ch.qos.logback:logback-classic:1.5.6")

    implementation("org.jetbrains.exposed:exposed-java-time:0.55.0")

    implementation("org.apache.pdfbox:pdfbox:2.0.30")
    implementation("com.itextpdf:itextpdf:5.5.13.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.gelov.betriebskosten.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
            )

            packageName = "Wohnungskostenabrechnung"
            packageVersion = "1.0.0"

            windows {
                // Optional: Icon-Datei (erst mal auskommentiert, bis du eine hast)
                // iconFile.set(project.file("src/main/resources/app-icon.ico"))
            }
        }
    }
}