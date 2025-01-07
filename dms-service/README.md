# SWIM DMS-Service

SWIM service for transferring files into DMS when notified from dispatch-service (Kafka)

## Architecture

```mermaid
flowchart LR
    Kafka --> DMS-Service
    DMS-Service --> DMS-REST-EAI
```
