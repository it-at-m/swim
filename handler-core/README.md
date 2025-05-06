# handler-core

Library with base components for building a new service for handling events from the `dispatch-service`

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

### Provided Out Ports

This package also provides following out ports with default functionality:
- [`FileSystemOutPort`](./src/main/java/de/muenchen/oss/swim/libs/handlercore/application/port/out/FileSystemOutPort.java):
  - Download a file from S3 via presigned URL
- [`FileEventOutPort`](./src/main/java/de/muenchen/oss/swim/libs/handlercore/application/port/out/FileEventOutPort.java):
  - Send a file finished event to the dispatch-service, which tags and moved the file for later cleanup in the S3
  - Needs to be called after successfully processing in the custom `ProcessFileInPort` implementation
  - For sending the according event, call the `fileFinished` method with the incoming event as argument

### Error handling

The dispatch-service provides an inbound topic for handling errors which occur during processing.
To use this functionality the Dead-Letter-Queue (dlq) needs to be configured for the incoming Consumer binding (See [Configuration](#configuration)).

After accordingly configured, all thrown exceptions are forwarded to the dispatch service, which handles them by sending a notification to the contact person of the corresponding use case and logging the error message.

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
          # use dispatch-service error handling for incoming events (optional)
          event-in-0:
            consumer:
              enable-dlq: true
              dlq-name:
      bindings:
        # incoming events
        event-in-0:
          group:
          destination:
        # finished event out
        finished-out:
          destination:
```

### Pattern

For properties which need to be resolved individually for each event (e.g. based on filename) the PatternHelper is available.

The used pattern requires a specific syntax (inspired by the sed command and regex substitution).

```
s/<regex>/<substitution>/<options>
```

The pattern is applied as following:
- `<regex>` is applied to input (filename without extension)
  - The extension is re-added where required (e.g. for the ContentObject name, as that is used to determine the file type)
- Build substitution values
  - Matching groups of regex are available via name and index
  - If option `m` is present metadata file is loaded
    - Values from `IndexFields` are available as `${if.<Name>}`
- Evaluate `<substitution>` and inject collected substitution values

Example:
- Filename: `Test-File.pdf` -> Input: `Test-File`
- Pattern: `s/^(.+)-(.+)$/${1}_${if.CustomValue}_${2}/m`
- Metadata file: `{"Document" : { "IndexFields" : [{ "Name": "CustomValue", "Value": "ExampleValue" }] } }`
- Result: `Test_ExampleValue_File`
