# SWIM DMS-Service

SWIM service for transferring files into DMS when notified by [dispatch-service](../dispatch-service) via Apache Kafka

## Architecture

```mermaid
flowchart LR
    Kafka --> DMS-Service
    DMS-Service --> DMS-REST-EAI
    DMS-Service --> Kafka
```
