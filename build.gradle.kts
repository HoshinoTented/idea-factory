plugins {
  kotlin("jvm") version "2.0.0"
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
    implementation(kotlin("stdlib"))
    implementation("org.glavo.kala:kala-common:0.80.0")
    testImplementation(kotlin("test"))
  }
  
  tasks.test {
    useJUnitPlatform()
  }
}

