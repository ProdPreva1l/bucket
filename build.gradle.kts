plugins {
    `maven-publish`
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.mongodb)
    api(libs.jetbrains.annotations)
}

tasks {
    withType<JavaCompile> {
        options.isFork = true
        options.encoding = "UTF-8"
        options.release = 21
    }

    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }

    register<Jar>("javadocJar") {
        val javadoc = named<Javadoc>("javadoc")
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(javadoc.get().destinationDir)
        group = "documentation"
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
        group = "documentation"
    }
}

publishing {
    publications {
        repositories.finallyADecentRepository()

        register<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
        }
    }
}

fun RepositoryHandler.finallyADecentRepository(
    name: String = "FinallyADecent",
    dev: Boolean = false,
    baseUrl: String = "https://repo.preva1l.info/",
) {
    maven("$baseUrl/${if (dev) "development" else "releases"}/") {
        this@maven.name = name
        credentials(PasswordCredentials::class)
    }
}