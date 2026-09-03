plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    version = libs.versions.mod.version.get()
    group = properties["maven_group"] as String
}

repositories {
    maven { name = "meteor-maven"; url = uri("https://maven.meteordev.org/releases") }
    maven { name = "meteor-maven-snapshots"; url = uri("https://maven.meteordev.org/snapshots") }
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.meteor.client)
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt())) }
}

fun minecraftCompatibility(version: String): String {
    val match = Regex("""^(\d{2})\.([1-9]\d*)(?:\.([1-9]\d*))?$""").matchEntire(version)
        ?: error("Invalid Minecraft version: $version")
    return "~${match.groupValues[1]}.${match.groupValues[2]}"
}

tasks.processResources {
    val values = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraftCompatibility(libs.versions.minecraft.get()),
        "jdk_version" to libs.versions.jdk.get()
    )
    inputs.properties(values)
    filesMatching("fabric.mod.json") { expand(values) }
}

tasks.jar {
    inputs.property("archivesName", project.base.archivesName.get())
    from("LICENSE") { rename { "${it}_${inputs.properties["archivesName"]}" } }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
