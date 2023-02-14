package com.example.demo.support;

import static io.restassured.RestAssured.given;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.specification.RequestSpecification;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

//Шаг 3. Создание тестового класса AbstractIntegrationTest
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

//Шаг 4. Подключение Postgres
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:tc:postgresql:14-alpine://testcontainers/workshop"
})
public class AbstractIntegrationTest {

    //Шаг 5. Подготовка для автотеста RestAssured
    protected RequestSpecification requestSpecification;

    @LocalServerPort
    protected int localServerPort;

    static final GenericContainer redis = new GenericContainer("redis:6-alpine")
            .withExposedPorts(6379);
    //Шаг 7. Добавление KafkaContainer
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:5.4.3"));

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withCopyFileToContainer(MountableFile.forClasspathResource("/schema.sql"), "/docker-entrypoint-initdb.d/");

    @DynamicPropertySource
    public static void configureRedis(DynamicPropertyRegistry registry) {
        Stream.of(redis, kafka, postgres).parallel().forEach(GenericContainer::start);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//        registry.add("spring.datasource.initialization-mode", () -> "never");
    }

    @BeforeEach
    public void setUpAbstractIntegrationTest() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        requestSpecification = new RequestSpecBuilder()
                .setPort(localServerPort)
                .addHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    @Test
    public void healthy() {
        given(requestSpecification)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .log().ifValidationFails(LogDetail.ALL);
    }

}
