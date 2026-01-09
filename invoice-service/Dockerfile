# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:973769d3afe5409197d90b67a261d1ff40fcf1593b35eb4d20556d67962d53a7

# Copy runnable jar to deployments
COPY target/invoice-service-*.jar /deployments/application.jar