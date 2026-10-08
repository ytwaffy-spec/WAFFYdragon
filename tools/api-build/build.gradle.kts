plugins { java }
repositories { mavenCentral() }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
sourceSets.main {
    java.srcDir(providers.gradleProperty("paperSource").map { "$it/paper-api/src/main/java" })
    java.srcDir(providers.gradleProperty("paperSource").map { "$it/paper-api/src/generated/java" })
    java.srcDir(providers.gradleProperty("brigadierSource").map { "$it/src/main/java" })
}
dependencies {
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.joml:joml:1.10.9")
    implementation("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.apache.logging.log4j:log4j-api:2.26.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("net.md-5:bungeecord-chat:1.21-R0.1")
    implementation(platform("net.kyori:adventure-bom:5.2.0"))
    implementation("net.kyori:adventure-api")
    implementation("net.kyori:adventure-key")
    implementation("net.kyori:adventure-text-minimessage")
    implementation("net.kyori:adventure-text-serializer-gson")
    implementation("net.kyori:adventure-text-serializer-legacy")
    implementation("net.kyori:adventure-text-serializer-plain")
    implementation("net.kyori:adventure-text-logger-slf4j")
    implementation("org.apache.maven:maven-resolver-provider:3.9.6")
    implementation("org.apache.maven.resolver:maven-resolver-connector-basic:1.9.18")
    implementation("org.apache.maven.resolver:maven-resolver-transport-http:1.9.18")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("org.checkerframework:checker-qual:4.2.3")
    implementation("org.jspecify:jspecify:1.0.0")
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(25); options.encoding = "UTF-8"; options.compilerArgs.add("-Xlint:none")
}
