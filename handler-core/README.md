# handler-core

Library with base componentes for building a new service for handling events from the `dispatch-service`

## Usage

Add this package as dependency:

```xml
<dependency>
    <groupId>de.muenchen.oss.swim</groupId>
    <artifactId>handler-core</artifactId>
    <version></version>
</dependency>
```

Create a custom implementation of the [`ProcessFileInPort`](./src/main/java/de/muenchen/oss/swim/libs/handlercore/application/port/in/ProcessFileInPort.java) interface. 

This package also provide following out ports with default functionality:
- [`FileSystemOutPort`](./src/main/java/de/muenchen/oss/swim/libs/handlercore/application/port/out/FileSystemOutPort.java):
  - Download a file from S3 via presigned URL
- [`FileEventOutPort`](./src/main/java/de/muenchen/oss/swim/libs/handlercore/application/port/out/FileEventOutPort.java):
  - Send a file finished event to the dispatch-service

## Configuration

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:29092
          configuration:
            security:
              protocol: PLAINTEXT
        bindings:
          # use dispatch-service error handling for event in (optional)
          <name>-in-0:
            consumer:
              enable-dlq: true
              dlq-name:
      bindings:
        # event in
        <name>-in-0:
          group:
          destination:
        # finished event out
        finished-out:
          destination:
```
