# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:b8be612ac873a91d2a9e05937453358e719b155dc0304c070ffe0e94cd6aa5e2

# Copy runnable jar to deployments
COPY target/dipa-service-*.jar /deployments/application.jar