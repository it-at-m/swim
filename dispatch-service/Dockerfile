# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:b0cbb0db7f04df615ba4b08c1c9cb69eb9805f6b319e127aa0e19be02314f821

# Copy runnable jar to deployments
COPY target/dispatch-service-*.jar /deployments/application.jar