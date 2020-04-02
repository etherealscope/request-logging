
description = "Spring boot starter for request logging"

dependencies {
    implementation(project(":request-logging-autoconfigure"))
}

val sourcesJar by tasks.creating(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks {
    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}

publishing {
    publications {
        create("maven", MavenPublication::class.java).apply {
            this.groupId = project.group.toString()
            this.artifactId = project.name
            this.version = project.version.toString()
            pom {
                description.set("Spring boot starter for request logging")
                name.set(project.name)
                url.set("https://github.com/etherealscope/request-logging")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("kubav182")
                        name.set("Jakub Venglar")
                        email.set("info@etherealscope.com")
                    }
                }
                scm {
                    url.set("https://github.com/etherealscope/request-logging")
                }
            }
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}