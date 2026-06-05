plugins {
    java
    application
}

group = "com.heater"
version = "0.2.0"
description = "Data Center Heater Side Gig — GPU waste-heat simulation for DAC and algae CO₂ removal"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.heater.App")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
