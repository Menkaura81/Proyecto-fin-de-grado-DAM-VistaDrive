import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.menkaura.vistadrive"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.menkaura.vistadrive"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.5"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

// Esto fue necesario para poder generar el Javadoc
tasks.register<Javadoc>("androidJavadoc") {
    val android = project.extensions.getByType(com.android.build.api.dsl.ApplicationExtension::class)

    source(android.sourceSets.getByName("main").java.directories)

    dependsOn("compileDebugJavaWithJavac")


    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    val sdkDir = localProperties.getProperty("sdk.dir")
    val compileSdkVersion = android.compileSdk
    if (sdkDir != null && compileSdkVersion != null) {
        classpath += project.files("$sdkDir/platforms/android-$compileSdkVersion/android.jar")
    }


    classpath += project.configurations.getByName("debugCompileClasspath").incoming.artifactView {
        attributes {
            attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }
    }.files


    classpath += project.files("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes")


    classpath += project.fileTree("${layout.buildDirectory.get()}/intermediates/compile_r_class_jar/debug") {
        include("**/*.jar")
    }

    destinationDir = file("${layout.buildDirectory.get()}/docs/javadoc/")

    options {
        encoding = "UTF-8"
        memberLevel = JavadocMemberLevel.PRIVATE
        (this as StandardJavadocDocletOptions).apply {
            charSet = "UTF-8"
            links("https://docs.oracle.com/javase/8/docs/api/")
            links("https://developer.android.com/reference/")
        }
    }

    exclude("**/R.java")
    exclude("**/BuildConfig.java")

    isFailOnError = false
}

secrets {
    // Optionally specify a different file name containing your secrets.
    // The plugin defaults to "local.properties"
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"

    // Configure which keys should be ignored by the plugin by providing regular expressions.
    // "sdk.dir" is ignored by default.
    ignoreList.add("keyToIgnore") // Ignore the key "keyToIgnore"
    ignoreList.add("sdk.*")       // Ignore all keys matching the regexp "sdk.*"
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.firebaseui:firebase-ui-auth:9.1.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore:26.1.1")
    implementation("androidx.navigation:navigation-ui:2.9.7")
    implementation("androidx.navigation:navigation-fragment:2.9.7")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}