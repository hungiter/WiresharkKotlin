plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.example.ptnet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ptnet"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")

        // Active ExternalNativeBuild flag
//        externalNativeBuild{
//            cmake{
//                cFlags("")
//            }
//        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-beta"
            isPseudoLocalesEnabled = true

            if (project.hasProperty("doNotStrip")) {
                androidComponents{
                    onVariants(selector().withBuildType("debug")){
                        packaging.jniLibs.keepDebugSymbols.add("**/libpcapd.so")
                        packaging.jniLibs.keepDebugSymbols.add("**/libcapture.so")
                    }
                }
//                Android Gradle Plugin < 7.0.0
//                packagingOptions {
//                    jniLibs {
//                        doNotStrip "**/libpcapd.so"
//                        doNotStrip("**/libcapture.so")
//                    }
//                }
            }
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable build config for CaptureService -> Utils
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Source set - Submodules
    sourceSets {
        getByName("main") {
            java.srcDirs("../submodules/MaxMind-DB-Reader-java/src/main/java")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // External native build -> C using
    externalNativeBuild{
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.preference)


    implementation(libs.gson)
    implementation(libs.dec)
    // Third-party
    implementation(libs.customactivityoncrash)
//    //Cannot found - use when need
//    implementation("com.github.KaKaVip:Android-Flag-Kit:v0.1")
//    implementation("com.github.AppIntro:AppIntro:6.2.0")
//    implementation("com.github.androidmads:QRGenerator:1.0.1")
}