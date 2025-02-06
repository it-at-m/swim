# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:cc4c076d6f7934154c4ebce97f92fd00218377c39cf7046560f0e24d5cbea8df

# Copy runnable jar to deployments
COPY target/dms-service-*.jar /deployments/application.jar