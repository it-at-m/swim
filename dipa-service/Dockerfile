# For documentation see https://jboss-container-images.github.io/openjdk/
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime@sha256:26acabfda6160ef348ceebba38e6616c43bf98a18800bf6afc20913556389c06

# Copy runnable jar to deployments
COPY target/dipa-service-*.jar /deployments/application.jar