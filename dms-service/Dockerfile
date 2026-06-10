# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:fe35e361a8ce4ea397ee0701f11c6a0f606142bdd4b0a58fd5b23c9b6b508f57

# Copy runnable jar to deployments
COPY target/dms-service-*.jar /deployments/application.jar