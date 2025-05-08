# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:bfc1e929ba7e729e908a99f3d1b64973ad378410b8b086b67304340cb0913167

# Copy runnable jar to deployments
COPY target/matching-service-*.jar /deployments/application.jar