plugins {
    id("java-library")
    id("maven-publish")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies{
    implementation(project(":DependencyManagerCore"))
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = "dtm.core"
            artifactId = "dependency-manager-annotations"
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
