plugins {
    id("java-library")
    id("maven-publish")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = "dtm.core"
            artifactId = "dependency-manager-core"
            version = "1.0.0"
        }
    }

    repositories {
        mavenLocal()
    }
}

tasks.named("publishReleasePublicationToMavenLocal") {
    dependsOn("jar")
    dependsOn("sourcesJar")
}