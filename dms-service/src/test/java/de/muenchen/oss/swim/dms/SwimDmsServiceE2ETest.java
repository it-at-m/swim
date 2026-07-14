package de.muenchen.oss.swim.dms;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import de.muenchen.oss.swim.libs.handlercore.domain.model.PresignedFile;
import de.muenchen.oss.swim.libs.handlercore.domain.model.SingleFileEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = SwimDmsServiceApplication.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@EmbeddedKafka(
        partitions = 1,
        topics = { SwimDmsServiceE2ETest.EVENT_TOPIC, SwimDmsServiceE2ETest.FINISHED_TOPIC, SwimDmsServiceE2ETest.ERROR_TOPIC },
        bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers"
)
@Testcontainers
class SwimDmsServiceE2ETest {
    /* default */ static final String EVENT_TOPIC = "swim-dms-e2e";
    /* default */ static final String FINISHED_TOPIC = "swim-finished-e2e";
    /* default */ static final String ERROR_TOPIC = "swim-dms-e2e-dlq";

    private static final String BUCKET = "swim-bucket";
    private static final String FILE_PATH = "test-path/test-COO.123.123.123-asd.pdf";
    private static final String METADATA_PATH = "test-path/test-COO.123.123.123-asd.json";
    private static final String USE_CASE = "e2e-metadata";
    private static final String DMS_RESPONSE_BODY = "{\"objid\":\"COO.2150.123.456\"}";
    private static final String METADATA_BODY = """
            {
              "Document": {
                "IndexFields": [
                  { "Name": "SWIM_DMS_Target", "Value": "procedure_incoming" },
                  { "Name": "VG_COO", "Value": "COO.2150.8801.2.1110045" },
                  { "Name": "VG_Username", "Value": "metadata.user" },
                  { "Name": "VG_Joboe", "Value": "metadata.joboe" },
                  { "Name": "VG_Jobposition", "Value": "metadata.jobposition" },
                  { "Name": "FdE_TestKey_1", "Value": "Test_Value_1" },
                  { "Name": "FdE_TestKey_2", "Value": "Test_Value_2" }
                ]
              }
            }
            """;

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "Test1234")
            .withCommand("server /data");

    private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(0);
    private static MinioClient minioClient;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeAll
    static void beforeAll() {
        WIRE_MOCK_SERVER.start();
        minioClient = MinioClient.builder()
                .endpoint("http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000))
                .credentials("minio", "Test1234")
                .build();
    }

    @AfterAll
    static void afterAll() throws Exception {
        WIRE_MOCK_SERVER.stop();
        minioClient.close();
    }

    @DynamicPropertySource
    /* default */ static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.stream.bindings.event-in-0.destination", () -> EVENT_TOPIC);
        registry.add("spring.cloud.stream.bindings.event-in-0.group", () -> "swim-dms-e2e");
        registry.add("spring.cloud.stream.bindings.finished-out.destination", () -> FINISHED_TOPIC);
        registry.add("spring.cloud.stream.output-bindings", () -> "finished-out");
        registry.add("spring.cloud.stream.kafka.bindings.event-in-0.consumer.enable-dlq", () -> "true");
        registry.add("spring.cloud.stream.kafka.bindings.event-in-0.consumer.dlq-name", () -> ERROR_TOPIC);
        registry.add("spring.cloud.stream.kafka.bindings.event-in-0.consumer.start-offset", () -> "earliest");
        registry.add("spring.cloud.stream.kafka.binder.configuration.security.protocol", () -> "PLAINTEXT");

        registry.add("swim.dms.base-url", WIRE_MOCK_SERVER::baseUrl);
        registry.add("swim.dms.username", () -> "dms-user");
        registry.add("swim.dms.password", () -> "dms-password");
        registry.add("swim.use-cases[0].name", () -> USE_CASE);
        registry.add("swim.use-cases[0].type", () -> "metadata_file");
        registry.add("swim.use-cases[0].coo-source.type", () -> "metadata_file");
        registry.add("swim.use-cases[0].incoming.metadata-subject", () -> "true");
        registry.add("swim.use-cases[0].incoming.incoming-name-pattern", () -> "s/^(.+)(?:-COO.[^-]+-)(.+)$/\\${2}/");
        registry.add("swim.use-cases[0].content-object.filename-overwrite-pattern", () -> "s/^(.+)(?:-COO.[^-]+-)(.+)$/\\${1}-\\${2}/");
    }

    @Test
    void shouldProcessMetadataDrivenFileEndToEnd() throws Exception {
        WIRE_MOCK_SERVER.stubFor(post(urlEqualTo("/incomings"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(DMS_RESPONSE_BODY)));
        putObject(FILE_PATH, "%PDF-1.4 test file".getBytes(StandardCharsets.UTF_8), "application/pdf");
        putObject(METADATA_PATH, METADATA_BODY.getBytes(StandardCharsets.UTF_8), "application/json");

        final SingleFileEvent event = new SingleFileEvent(
                USE_CASE,
                new PresignedFile(presignedUrl(FILE_PATH), presignedUrl(METADATA_PATH)));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(EVENT_TOPIC, objectMapper.writeValueAsString(event))).get();
        }

        final SingleFileEvent finishedEvent = readFinishedEvent();

        assertThat(finishedEvent).isEqualTo(event);
        WIRE_MOCK_SERVER.verify(postRequestedFor(urlEqualTo("/incomings"))
                .withHeader("x-anwendung", equalTo("SWIM"))
                .withHeader("userlogin", equalTo("metadata.user"))
                .withHeader("joboe", equalTo("metadata.joboe"))
                .withHeader("jobposition", equalTo("metadata.jobposition"))
                .withRequestBody(containing("COO.2150.8801.2.1110045"))
                .withRequestBody(containing("asd"))
                .withRequestBody(containing("Test_Value_1 (TestKey_1)"))
                .withRequestBody(containing("Test_Value_2 (TestKey_2)"))
                .withRequestBody(containing("test-asd.pdf"))
                .withRequestBody(containing("%PDF-1.4 test file")));
        assertThat(meterRegistry.counter("swim_dms_processed_count", "use-case", USE_CASE, "dms-type", "METADATA_FILE").count()).isEqualTo(1.0);
        assertThat(WIRE_MOCK_SERVER.findUnmatchedRequests().getRequests()).isEmpty();
    }

    @Test
    void shouldSendFailedEventToDlq() throws Exception {
        final SingleFileEvent event = new SingleFileEvent(
                "unknown-use-case",
                new PresignedFile("http://localhost:9000/%s/%s".formatted(BUCKET, FILE_PATH), null));

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps())) {
            producer.send(new ProducerRecord<>(EVENT_TOPIC, objectMapper.writeValueAsString(event))).get();
        }

        final SingleFileEvent errorEvent = readEvent(ERROR_TOPIC, "swim-dms-error-e2e");

        assertThat(errorEvent).isEqualTo(event);
    }

    private SingleFileEvent readFinishedEvent() {
        return readEvent(FINISHED_TOPIC, "swim-dms-finished-e2e");
    }

    private SingleFileEvent readEvent(final String topic, final String groupId) {
        final Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(groupId, "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, topic);
            final ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(30));
            return objectMapper.readValue(record.value(), SingleFileEvent.class);
        } catch (final Exception e) {
            throw new AssertionError("Event could not be read from topic " + topic, e);
        }
    }

    private Map<String, Object> producerProps() {
        final Map<String, Object> props = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker.getBrokersAsString()));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    private void putObject(final String path, final byte[] content, final String contentType) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
        }
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(BUCKET)
                .object(path)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .contentType(contentType)
                .build());
    }

    private String presignedUrl(final String path) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(BUCKET)
                .object(path)
                .build());
    }
}
