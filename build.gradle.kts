plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "fr.bananasmoothii"
version = "1.0-SNAPSHOT"

sourceSets {
    main {
        java {
            srcDir("MCWFC/src/main/java")
        }
    }
    test {
        java {
            srcDir("MCWFC/src/test/java")
        }
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.bananasmoothii:Minestom:master-SNAPSHOT")
    implementation("org.tinylog:slf4j-tinylog:2.4.1")
    compileOnly("org.jetbrains:annotations:23.0.0")
    implementation("net.kyori:adventure-text-minimessage:4.11.0")
}

tasks {
    jar {
        manifest {
            attributes(
                "Main-Class" to "fr.bananasmoothii.minestomtest.Main",
                "Muti-Release" to "true",
            )
        }
    }
}
