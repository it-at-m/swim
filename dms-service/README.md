# SWIM DMS-Service

SWIM service for transferring files into DMS when notified by [dispatch-service](../dispatch-service) via Apache Kafka

## Architecture

```mermaid
flowchart LR
    Kafka --> DMS-Service
    DMS-Service --> DMS-REST-EAI
    DMS-Service --> Kafka
```

For the DMS-REST-EAI the [`refarch-dms-integration-fabasoft-rest-api` module](https://github.com/it-at-m/refarch/tree/main/refarch-integrations/refarch-dms-integration/refarch-dms-integration-fabasoft-rest-api) is used. 

## Development

- The dms-service is built with JDK21
- For local development and testing the dev docker-compose stack of the [dispatch-service](../dispatch-service) can be used.
    - Can be started with `docker compose up -d`
- The Spring profile `local` is preconfigured for using the stack
    - Activate it either manually or by using the provided run configuration
- After starting the application, file processing can be triggered via Kafka in one of the following ways:
    - via [dispatch-service](../dispatch-service)
    - via [Kafka-UI](http://localhost:8089/)

## Configuration

```yaml
# dms connection configuration
swim:
  dms:
    base-url:
    username:
    password:
  # use cases (Correct combinations are documented within domain.model.UseCase class)
  use-cases:
    - name: # required
      type: # <inbox|incoming_object> required
      coo-source: # <metadata_file|static|ou_default> required
      username:
      joboe:
      jobposition:
      target-coo: # required for coo-source static
```
