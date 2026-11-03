# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi10/openjdk-21-runtime:1.24-18@sha256:fabfabf99c4b39afa238f92c6fd48da485ae4791ab35cb1484c8d20bb99bcb89

# Copy runnable jar to deployments
COPY target/dispatch-service-*.jar /deployments/application.jar