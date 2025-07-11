# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:02bee3ed743662399f823584ef5eaeb0a8e2b55ddb2766bcde83560d810c5958

# Copy runnable jar to deployments
COPY target/matching-service-*.jar /deployments/application.jar