# SWIM Invoice-Service

SWIM service for transferring files (invoices) to SAP when notified by [dispatch-service](../dispatch-service) via Apache Kafka.
Based on [handler-core](../handler-core).

## Architecture

```mermaid
flowchart LR
    Kafka --> Invoice-Service --> SAP-PO --> SAP
    Invoice-Service --> Kafka
```

### Error handling

Errors which occur during processing a file are transmitted via Kafka to the [dispatch-service](../dispatch-service) which handles them accordingly.

## Development

- The invoice-service is built with JDK21
- For local development and testing the dev docker-compose stack of the [dispatch-service](../dispatch-service) can be used.
    - Can be started with `docker compose up -d`
- The Spring profile `local` is preconfigured for using the stack
    - Activate it either manually or by using the provided run configuration
    - Additionally, the sap-po credentials need to be configured in the [`application-local.yml`](./src/main/resources/application-local.yml). See [Configuration](#configuration) for reference.
- After starting the application, file processing can be triggered via Kafka in one of the following ways:
    - via [dispatch-service](../dispatch-service)
    - via [Kafka-UI](http://localhost:8089/)

## Configuration

```yaml
swim:
  # sap-po connection configuration
  sap:
    endpoint-url:
    username:
    password:
    filename-pattern: "([^-]+)-([^-]+)-([^-]+)-?(.*).pdf" # See Filename syntax (default)
    info-pagination-key: "Paginier_Nummer" # key for pagination nr in additional invoice information (default)
    info-barcode-key: "Barcode" # key for barcode in additional invoice information (default)
```

## Filename syntax

The invoice-service processes the file by extracting required information from the input filename.
The pattern to extract the information can be configured. See [Configuration](#configuration) `filename-pattern`.
It needs to contain four matching groups which are resolved as shown in following example:

```
Default Regex: ([^-]+)-([^-]+)-([^-]+)-?(.*).pdf
<document type>-<pagination nr>-<box nr>-<barcode>.pdf
```

- Document type: type of the input document
  - Allowed values: `REC` (short for german "Rechnung") and `RBU` (short for german "rechnungsbegleitende Unterlage")
- Pagination number (German "Paginiernummer")
- Box number (German "Kistennummer")
- Barcode
  - Only required for document type `RBU`
