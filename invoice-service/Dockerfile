# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:84a136ce036ebde3adc502c84c39f22bd4bf14ca387a3825cf3e1c7ae26c0942

# Copy runnable jar to deployments
COPY target/invoice-service-*.jar /deployments/application.jar