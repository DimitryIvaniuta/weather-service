plugins {
    id("java")
    id("org.springframework.boot") version "3.4.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.weather"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories { mavenCentral() }

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // Spring Security core + web security
    implementation("org.springframework.boot:spring-boot-starter-security")
    // Provides @EnableMethodSecurity, MethodSecurityConfigurer, etc.
    implementation("org.springframework.security:spring-security-config")

    // reactive WebClient
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // core retry + circuit-breaker logic
    implementation("io.github.resilience4j:resilience4j-retry:2.3.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.3.0")

    // annotations for Spring Boot 3
    implementation("io.github.resilience4j:resilience4j-annotations:2.3.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.3.0")

    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Redis (L2)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Observability
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // Lombok for tests
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.0"))

    // Core library (GenericContainer, Docker client, etc.)
    testImplementation("org.testcontainers:testcontainers:1.21.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test { useJUnitPlatform() }
