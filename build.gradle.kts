plugins {
  kotlin("jvm") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
  apply {
    plugin("kotlin")
  }
  
  repositories {
    mavenCentral()
  }
  
  dependencies {
    implementation("org.glavo.kala:kala-common:0.70.0")
    testImplementation(kotlin("test"))
  }
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}