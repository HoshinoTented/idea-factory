plugins {
  java
  kotlin("jvm") version "2.0.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
  repositories {
    mavenCentral()
  }
}

dependencies {
  implementation(kotlin("stdlib"))
}

subprojects {
  apply {
    plugin("java")
  }
  
  dependencies {
    implementation("org.glavo.kala:kala-common:0.70.0")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  }
  
  tasks.test {
    useJUnitPlatform()
  }
}
