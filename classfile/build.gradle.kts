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