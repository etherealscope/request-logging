import java.util.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

plugins {
    java
    maven
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.1"
    id("org.springframework.boot") version "2.2.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

tasks {
    getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        enabled = false
    }

    getByName<Jar>("jar") {
        enabled = false
    }
}

subprojects {
    buildscript {
        repositories {
            mavenLocal()
            mavenCentral()
            jcenter()
        }
    }

    group = "com.etherealscope"
    version = "4.2.0"

    apply(plugin="java")
    apply(plugin="maven")
    apply(plugin="maven-publish")
    apply(plugin="io.spring.dependency-management")
    apply(plugin="org.springframework.boot")
    apply(plugin="com.jfrog.bintray")

    repositories {
        mavenCentral()
        mavenLocal()
        jcenter()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.2.0.RELEASE")
        }
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
        getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
            enabled = false
        }

        getByName<Jar>("jar") {
            enabled = true
        }
    }

    configure<com.jfrog.bintray.gradle.BintrayExtension> {
        user = System.getProperty("bintray.user")
        key = System.getProperty("bintray.key")
        publish = true
        setPublications("maven")
        pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
            repo = "maven"
            name = project.name
            userOrg = "etherealscope"
            websiteUrl = "https://github.com/etherealscope/request-logging"
            githubRepo = "etherealscope/request-logging"
            vcsUrl = "https://github.com/etherealscope/request-logging"
            description = project.description
            setLabels("java")
            setLicenses("Apache-2.0")
            desc = description
            version(closureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
                this.name = project.version.toString()
                released = Date().toString()
            })
        })
    }

}