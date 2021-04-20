repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.springframework.boot") version "2.4.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
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

    group = "com.etherealscope"
    version = "5.0.0"

    apply(plugin="java-library")
    apply(plugin="maven-publish")
    apply(plugin="signing")
    apply(plugin="io.spring.dependency-management")
    apply(plugin="org.springframework.boot")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:2.4.5")
        }
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks {
        getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
            enabled = false
        }

        getByName<Jar>("jar") {
            enabled = true
        }
    }

    publishing {
        repositories {
            maven {
                name = "central"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.property("ossrhUsername").toString() //System.getProperty("ossrhUsername")
                    password = project.property("ossrhPassword").toString() //System.getProperty("ossrhPassword")
                }
            }
            maven {
                name = "centralSnapshot"
                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                credentials {
                    username = project.property("ossrhUsername").toString()
                    password = project.property("ossrhPassword").toString()
                }
            }
        }
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()

                from(components["java"])

                versionMapping {
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }

                pom {
                    name.set("Spring Boot Request Logging")
                    description.set("Spring Boot starter for enhanced http request logging")
                    url.set("https://github.com/etherealscope/request-logging")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("jakubv")
                            name.set("Jakub Venglar")
                            email.set("info@etherealscope.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/etherealscope/request-logging.git")
                        developerConnection.set("scm:git:ssh://github.com/etherealscope/request-logging.git")
                        url.set("https://github.com/etherealscope/request-logging")
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }

}