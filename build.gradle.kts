plugins {
    id("java")
}

group = "info.preva1l.bucket"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:mongodb-driver-sync:5.5.1")
    compileOnly("org.jetbrains:annotations:24.0.1")
}