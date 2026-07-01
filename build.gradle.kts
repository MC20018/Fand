plugins {
    `java-library`
    id("io.papermc.paperweight.core") version "2.0.0-SNAPSHOT" apply false
}

val releaseVersion = providers.gradleProperty("releaseVersion").orElse("0.1.4-SNAPSHOT").get()

allprojects {
    group = "io.fand"
    version = releaseVersion
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        withSourcesJar()
        withJavadocJar()
    }

    repositories {
        maven("https://repo.fandmc.cn/repository/maven-public/")
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.10.2")
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs(
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
        )
    }
}
