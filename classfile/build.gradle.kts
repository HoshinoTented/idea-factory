import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply {
  plugin("kotlin")
}

dependencies {
  implementation(kotlin("stdlib"))
//  implementation(kotlin("reflect"))
}

tasks.withType<JavaCompile> {
  options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
  jvmArgs = listOf("--enable-preview")
}

tasks.withType<JavaExec> {
  jvmArgs = listOf("--enable-preview")
}

val javaVersion = JavaVersion.VERSION_22

java {
  toolchain {
    targetCompatibility = javaVersion
    sourceCompatibility = javaVersion
  }
}

tasks.withType<KotlinCompile> {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_22)
}