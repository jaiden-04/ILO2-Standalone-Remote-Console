plugins {
    java
    application
}

application {
    mainClass.set("Main")
    applicationDefaultJvmArgs = listOf("-Djava.security.properties=java.security")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "Main")
    }
}

// fix compilation as long as literals are not properly cleaned up in source code (LocaleTranslator especially)
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}
