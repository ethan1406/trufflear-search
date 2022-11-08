import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
	kotlin("jvm") version "1.6.21"
	id("com.google.protobuf") version "0.8.18"
	id("org.springframework.boot") version "2.7.0"
}

group = "com.trufflear"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

ext["grpcVersion"] = "1.46.0"
ext["grpcKotlinVersion"] = "1.3.0" // CURRENT_GRPC_KOTLIN_VERSION
ext["protobufVersion"] = "3.21.0"
ext["coroutinesVersion"] = "1.6.1"

repositories {
	mavenCentral()
	flatDir {
		dirs("libs")
	}
}

sourceSets {
	val main by getting { }
	main.java.srcDirs("build/generated/source/proto/main/java")
	main.java.srcDirs("build/generated/source/proto/main/grpc")
	main.java.srcDirs("build/generated/source/proto/main/kotlin")
	main.java.srcDirs("build/generated/source/proto/main/grpckt")
}

dependencies {


	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.ext["coroutinesVersion"]}")
	implementation("io.grpc:grpc-stub:${rootProject.ext["grpcVersion"]}")
	implementation("io.grpc:grpc-protobuf:${rootProject.ext["grpcVersion"]}")
	implementation("com.google.protobuf:protobuf-kotlin:${rootProject.ext["protobufVersion"]}")
	implementation("io.grpc:grpc-kotlin-stub:${rootProject.ext["grpcKotlinVersion"]}")
	runtimeOnly("io.grpc:grpc-netty:${rootProject.ext["grpcVersion"]}")

	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation("org.jetbrains.exposed:exposed-core:0.38.2")
	implementation("org.jetbrains.exposed:exposed-dao:0.38.2")
	implementation("org.jetbrains.exposed:exposed-jdbc:0.38.2")
	implementation("mysql:mysql-connector-java:8.0.29")
	implementation("com.zaxxer:HikariCP:5.0.1")

	implementation("com.amazonaws:aws-java-sdk-s3:1.12.332")
	implementation("org.bitbucket.b_c:jose4j:0.9.2")
	implementation ("com.google.code.gson:gson:2.10")

	//implementation(files("libs/typesense-java-0.0.3.jar"))

	testImplementation("org.assertj:assertj-core:3.23.1")
	testImplementation ("org.junit.jupiter:junit-jupiter-api:5.9.0")
	testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.9.0")


}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}


tasks.withType<Test> {
	useJUnitPlatform()
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:${rootProject.ext["protobufVersion"]}"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:${rootProject.ext["grpcVersion"]}"
		}
		id("grpckt") {
			artifact = "io.grpc:protoc-gen-grpc-kotlin:${rootProject.ext["grpcKotlinVersion"]}:jdk8@jar"
		}
	}
	generateProtoTasks {
		ofSourceSet("main").forEach {
			it.plugins {
				id("grpc")
				id("grpckt")
			}
			it.builtins {
				id("kotlin")
			}
		}
	}
}