plugins {
  `java-library`

  pmd
  idea

  `maven-publish`
  signing

  // https://plugins.gradle.org/plugin/io.github.gradle-nexus.publish-plugin
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
  // https://plugins.gradle.org/plugin/com.diffplug.spotless
  id("com.diffplug.spotless") version "6.19.0"
  // https://plugins.gradle.org/plugin/me.champeau.jmh
  id("me.champeau.jmh") version "0.7.1"
}

group = "de.tum.in"

version = "0.6.0"

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11

  withSourcesJar()
  withJavadocJar()
}

var defaultEncoding = "UTF-8"

tasks.withType<JavaCompile> { options.encoding = defaultEncoding }

tasks.withType<Javadoc> {
  options.encoding = defaultEncoding
  options {
    this as StandardJavadocDocletOptions
    addBooleanOption("Xdoclint:all,-missing", true)
  }
}

tasks.withType<Test> { systemProperty("file.encoding", "UTF-8") }

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
  }
}

repositories { mavenCentral() }

spotless {
  java {
    licenseHeaderFile("${project.rootDir}/config/LICENCE_HEADER")
    palantirJavaFormat()
  }
  kotlinGradle { ktfmt() }
}

jmh { includeTests.set(true) }

dependencies {
  compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.3")

  // https://mvnrepository.com/artifact/com.google.guava/guava
  testImplementation("com.google.guava:guava:33.4.8-jre")
  // https://mvnrepository.com/artifact/org.hamcrest/hamcrest
  testImplementation("org.hamcrest:hamcrest:3.0")
  // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0-M3")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.13.0-M3")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.0-M3")

  // https://mvnrepository.com/artifact/org.immutables/value
  compileOnly("org.immutables:value:2.10.1:annotations")
  annotationProcessor("org.immutables:value:2.10.1")

  jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
  useJUnitPlatform()
  minHeapSize = "2g"
  maxHeapSize = "16g"
}

// PMD
// https://docs.gradle.org/current/dsl/org.gradle.api.plugins.quality.Pmd.html

pmd {
  toolVersion = "6.55.0" // https://pmd.github.io/
  reportsDir = layout.buildDirectory.file("reports/pmd").get().asFile
  ruleSetFiles = files("${project.rootDir}/config/pmd-rules.xml")
  ruleSets = listOf() // We specify all rules in rules.xml
  isConsoleOutput = false
  isIgnoreFailures = false
}

tasks.withType<Pmd> {
  reports {
    xml.required.set(false)
    html.required.set(true)
  }
}

// Deployment - run with -Prelease clean publishToSonatype closeAndReleaseSonatypeStagingRepository
// Authentication: sonatypeUsername+sonatypePassword in ~/.gradle/gradle.properties
if (project.hasProperty("release")) {
  publishing {
    publications {
      create<MavenPublication>("mavenJava") {
        from(project.components["java"])

        signing {
          useGpgCmd()
          sign(publishing.publications)
        }

        pom {
          name.set("JBDD")
          description.set("Pure Java implementation of (Binary) Decision Diagrams")
          url.set("https://github.com/incaseoftrouble/jbdd")

          licenses {
            license {
              name.set("The GNU General Public License, Version 3")
              url.set("https://www.gnu.org/licenses/gpl.txt")
            }
          }

          developers {
            developer {
              id.set("incaseoftrouble")
              name.set("Tobias Meggendorfer")
              email.set("tobias@meggendorfer.de")
              url.set("https://github.com/incaseoftrouble")
              timezone.set("Europe/Berlin")
            }
          }

          scm {
            connection.set("scm:git:https://github.com/incaseoftrouble/jbdd.git")
            developerConnection.set("scm:git:git@github.com:incaseoftrouble/jbdd.git")
            url.set("https://github.com/incaseoftrouble/jbdd")
          }
        }
      }
    }
  }

  nexusPublishing { repositories.sonatype() }
}
