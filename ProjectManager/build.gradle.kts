plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "dtm.core"
version = "1.0.0"

android {
    namespace = "dtm.core.androidprojectmanager"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":DependencyManager"))
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dtm.core"
            artifactId = "androidprojectmanager"
            version = "1.0.0"

            afterEvaluate {
                artifact(
                    layout.buildDirectory.file("outputs/aar/${project.name}-release.aar")
                        .get().asFile
                )
            }
        }
    }

    repositories {
        mavenLocal()
    }
}

tasks.named("publishReleasePublicationToMavenLocal") {
    dependsOn("bundleReleaseAar")
}