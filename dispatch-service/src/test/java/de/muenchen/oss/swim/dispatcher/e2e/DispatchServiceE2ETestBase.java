package de.muenchen.oss.swim.dispatcher.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import de.muenchen.oss.swim.dispatcher.SwimDispatcherServiceApplication;
import de.muenchen.oss.swim.dispatcher.TestConstants;
import de.muenchen.oss.swim.dispatcher.domain.model.PresignedFile;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.FileEvent;
import de.muenchen.oss.swim.dispatcher.domain.model.streaming.MultiFileEvent;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectTagsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetObjectTagsArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import io.minio.messages.Tags;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = SwimDispatcherServiceApplication.class)
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@EmbeddedKafka(
        partitions = 1,
        topics = { DispatchServiceE2ETestBase.FINISHED_TOPIC, DispatchServiceE2ETestBase.DLQ_TOPIC, DispatchServiceE2ETestBase.DISPATCH_TOPIC },
        bootstrapServersProperty = "spring.cloud.stream.kafka.binder.brokers"
)
@SuppressWarnings({ "PMD.DoNotUseThreads", "PMD.AvoidUsingHardCodedIP" })
class DispatchServiceE2ETestBase {
    protected static final String FINISHED_TOPIC = "swim-dispatch-finished-e2e";
    protected static final String DLQ_TOPIC = "swim-dispatch-dlq-e2e";
    protected static final String DISPATCH_TOPIC = "swim-dispatch-output-e2e";
    protected static final String BUCKET = "test-bucket";
    protected static final String USE_CASE = "test-meta";
    private static final String DATABASE_NAME = "swim";
    private static final List<String> TEST_BUCKETS = List.of(BUCKET, "test-bucket-2", "test-bucket-3");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z"))
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "Test1234")
            .withCommand("server /data");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", DATABASE_NAME)
            .withEnv("POSTGRES_USER", "swim")
            .withEnv("POSTGRES_PASSWORD", "swim");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>(DockerImageName.parse("axllent/mailpit:v1.21.8"))
            .withExposedPorts(1025, 8025);

    static {
        MINIO.start();
        POSTGRES.start();
        MAILPIT.start();
    }

    private static MinioClient minioClient;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private JsonMapper objectMapper;

    @BeforeAll
    static void setUpInfrastructure() throws Exception {
        minioClient = MinioClient.builder()
                .endpoint("http://127.0.0.1:" + MINIO.getMappedPort(9000))
                .credentials("minio", "Test1234")
                .build();
        for (final String bucket : TEST_BUCKETS) {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        }
    }

    @AfterAll
    static void tearDownInfrastructure() throws Exception {
        minioClient.close();
    }

    @DynamicPropertySource
    /* default */ static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.function.definition", () -> "finished;dlq");
        registry.add("spring.cloud.stream.bindings.finished-in-0.destination", () -> FINISHED_TOPIC);
        registry.add("spring.cloud.stream.bindings.finished-in-0.group", () -> "dispatch-finished-e2e");
        registry.add("spring.cloud.stream.kafka.bindings.finished-in-0.consumer.start-offset", () -> "earliest");
        registry.add("spring.cloud.stream.bindings.dlq-in-0.destination", () -> DLQ_TOPIC);
        registry.add("spring.cloud.stream.bindings.dlq-in-0.group", () -> "dispatch-dlq-e2e");
        registry.add("spring.cloud.stream.kafka.bindings.dlq-in-0.consumer.start-offset", () -> "earliest");
        registry.add("spring.cloud.stream.bindings.dms-out.destination", () -> DISPATCH_TOPIC);
        registry.add("spring.cloud.stream.kafka.binder.configuration.security.protocol", () -> "PLAINTEXT");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/" + DATABASE_NAME);
        registry.add("spring.datasource.username", () -> DATABASE_NAME);
        registry.add("spring.datasource.password", () -> DATABASE_NAME);
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
        registry.add("swim.s3.url", () -> "http://127.0.0.1:" + MINIO.getMappedPort(9000));
        registry.add("swim.s3.access-key", () -> "minio");
        registry.add("swim.s3.secret-key", () -> "Test1234");
        registry.add("swim.dispatching-cron", () -> "-");
        registry.add("swim.protocol-processing-cron", () -> "-");
    }

    protected void sendEvent(final String topic, final FileEvent event, final Map<String, String> headers) throws Exception {
        final Map<String, Object> producerProperties = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker.getBrokersAsString()));
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties)) {
            final ProducerRecord<String, String> record = new ProducerRecord<>(topic, objectMapper.writeValueAsString(event));
            headers.forEach((key, value) -> record.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8))));
            producer.send(record).get(30, TimeUnit.SECONDS);
        }
    }

    protected void awaitConsumerGroup(final String group) throws Exception {
        final Map<String, Object> adminProperties = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            while (System.nanoTime() < deadline) {
                try {
                    if (!adminClient.describeConsumerGroups(List.of(group)).all().get(1, TimeUnit.SECONDS)
                            .get(group).members().isEmpty()) {
                        return;
                    }
                } catch (final Exception ignored) {
                    // The embedded broker may still be initializing its group coordinator.
                }
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Kafka consumer group did not become ready: " + group);
    }

    @SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
    protected MultiFileEvent receiveMultiFileEvent(final String group) {
        final Map<String, Object> consumerProperties = new HashMap<>(KafkaTestUtils.consumerProps(embeddedKafkaBroker, group, true));
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
            consumer.subscribe(List.of(DISPATCH_TOPIC));
            final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (System.nanoTime() < deadline) {
                for (final ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(100))) {
                    return objectMapper.readValue(record.value(), MultiFileEvent.class);
                }
            }
        }
        throw new AssertionError("No multi-file dispatch event was received");
    }

    protected String presignedUrl(final String path) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(BUCKET)
                .object(path)
                .build());
    }

    protected void putObject(final String path, final byte[] content) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(BUCKET)
                .object(path)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .build());
    }

    protected void tagObject(final String path, final Map<String, String> tags) throws Exception {
        minioClient.setObjectTags(SetObjectTagsArgs.builder()
                .bucket(BUCKET)
                .object(path)
                .tags(tags)
                .build());
    }

    protected Tags tags(final String path) throws Exception {
        return minioClient.getObjectTags(GetObjectTagsArgs.builder().bucket(BUCKET).object(path).build());
    }

    protected boolean objectExists(final String path) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(BUCKET).object(path).build());
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    protected void awaitObjectState(final String path, final boolean expected) throws InterruptedException {
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline && objectExists(path) != expected) {
            Thread.sleep(100);
        }
        assertThat(objectExists(path)).isEqualTo(expected);
    }

    protected void awaitTag(final String path, final String key, final String expected) throws Exception {
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (expected.equals(tags(path).get().get(key))) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(tags(path).get().get(key)).isEqualTo(expected);
    }

    protected void awaitMail(final String recipient, final String expectedContent) throws Exception {
        final URI uri = URI.create("http://127.0.0.1:" + MAILPIT.getMappedPort(8025) + "/api/v1/messages");
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            final HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.body().contains(recipient) && response.body().contains(expectedContent)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Mail was not delivered to " + recipient + " with content " + expectedContent);
    }

    protected PresignedFile presignedFile(final String path) throws Exception {
        return new PresignedFile(presignedUrl(path), null);
    }
}
