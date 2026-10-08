plugins { java }
group = "dev.waffy.babydragons"
version = "0.67.A"
repositories { mavenCentral(); maven("https://repo.papermc.io/repository/maven-public/") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
dependencies {
    val localApi = providers.gradleProperty("localPaperApi").orNull
        ?: file(".local/paper-api-source-build.jar").takeIf { it.exists() }?.path
    if (localApi != null) { compileOnly(files(localApi)); testImplementation(files(localApi)) }
    else { compileOnly("io.papermc.paper:paper-api:26.3.+"); testImplementation("io.papermc.paper:paper-api:26.3.+") }
    implementation("net.kyori:adventure-api:5.2.0")
    implementation("net.kyori:adventure-text-minimessage:5.2.0")
    compileOnly("org.joml:joml:1.10.9")
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.1")
    testImplementation("org.yaml:snakeyaml:2.2")
    testImplementation("com.google.guava:guava:33.6.0-jre")
    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.withType<JavaCompile>().configureEach { options.release.set(25); options.encoding = "UTF-8" }
tasks.test { useJUnitPlatform() }
tasks.jar {
    archiveFileName.set("BaByDragons 0.67.A.jar")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
