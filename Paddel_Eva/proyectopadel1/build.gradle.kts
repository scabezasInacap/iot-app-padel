buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")  // o la versión que uses
        classpath(kotlin("gradle-plugin", version = "1.9.0"))  // Kotlin plugin
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

