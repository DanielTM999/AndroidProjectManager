plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "dtm.core"
version = "1.0.0"

android {
    namespace = "dtm.core.dependencymanager"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dtm.core"
            artifactId = "dependency-manager"
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