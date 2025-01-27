plugins {
  kotlin("jvm") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val junitVersion = "5.11.4"

allprojects {
  apply {
    plugin("kotlin")
  }
  
  repositories {
    mavenCentral()
  }
  
  dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.glavo.kala:kala-common:0.80.0")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter", "junit-jupiter")
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
  }
  
  tasks.test {
    useJUnitPlatform()
  }
}

