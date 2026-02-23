# SWIM Dispatcher-Service

SWIM service for notifying other services (i.e. DMS) that a file is ready for further processing via Kafka.

## Architecture

```mermaid
flowchart LR
    Schedule --> Dispatcher
    Dispatcher --> S3
    Dispatcher --> Kafka
    Dispatcher --> Mail
    Dispatcher --> Protocol-DB
```

## Development

- The swim-dispatch-service is built with JDK21
- For local development and testing a dev docker-compose stack is provided in `./stack`
    - Can be started with `docker compose up -d`
- The Spring profile `local` is preconfigured for using the stack
    - Activate it either manually or by using the provided run configuration
- After starting the following UIs are available:
  - [S3/Minio](http://localhost:9001/) (User: `minio`, PW: `Test1234`)
  - [Mailpit](http://localhost:8025/)
  - [Kafka-UI](http://localhost:8089/)
  - [pgAdmin](http://localhost:5050/)

## Configuration

In addition to the properties listed below, other Spring libraries must also be configured (e.g. Mail, DB).
These properties and example values can be found in the [`application-local.yml`](./src/main/resources/application-local.yml).

```yaml
swim:
  dispatching-cron: # cron interval for triggering dispatching
  protocol-processing-cron: # cron interval for triggering protocol processing
  fallback-mail: # fallback mail used for notification mails if use case can't be resolved
  mail:
    from-address: # mail address used for sending notifications
    mail-subject-prefix: # prefix added to mail subject (e.g. for specifying the environment)
    locale: # change the language of the mails (optional, default: en, alternatives: de)
  # s3 connection options
  s3:
    url:
    access-key:
    secret-key:
  # dirs
  dispatch-folder: inProcess # subfolder to search for files to process under
  finished-folder: finished # subfolder to move finished files to
  protocol-finished-folder: finishedProtocols # subfolder to move finished protocols to
  # metadata
  metadata-dispatch-binding-key: SWIM_Dispatch_Target # see Section "Rerouting"
  # s3 tags
  dispatch-state-tag-key: SWIM_State # tag to use for processing state
  dispatched-state-tag-value: sentToKafka # state if a file was processed and sent to kafka
  dispatch-file-finished-tag-value: finished # state if a file is finished
  dispatch-action-tag-key: SWIM_Action # see Section "Rerouting"
  dispatch-action-destination-tag-key: SWIM_Reroute_Destination # see Section "Rerouting"
  protocol-state-tag-key: CSV_State # tag to use for processing state of protocol CSVs
  protocol-match-tag-key: CSV_Match # tag to mark how a protocol matches the files (correct, missingFiles, missingInProtocol, missingInProtocolAndFiles)
  protocol-processed-state-tag-value: finished # state if a protocol file was finished
  protocol-processed-files-state-tag-value: protocolProcessingSuccessful # state for files which were contained in a successful protocol, see use-cases.tag-protocol-processed
  error-state-value: error # state if error occurred
  error-class-tag-key: errorClass # tag for error class
  error-message-tag-key: errorMessage # tag for error message
  # list of use cases
  use-cases:
    - name: # name of the use case
      bucket: # bucket to look for new files in
      path: # path to look for new files under
      recursive: # if the file lookup should be recursive (optional, default: false)
      max-file-size: # max size files can have that they are dispatched (optional, default: 90MB (IEC) -> 90*1024*1024B, example: 1GB)
      required-tags: # map of tags required on files to be dispatched (optional, default: {})
      requires-metadata: # if a metadata file is required (optional, default: false)
      destination-binding: # the target destination binding, see section "Adding additional target"
      overwrite-destination-via-metadata: # if the destination binding should be resolved via the metadata file (optional, default: false, fallback to destination-binding)
      mail-addresses: # list of mail addresses used for sending notifications
      sensitive-filename: # if the filename is sensitive, if true it isn't logged (optional, default: false)
      protocol-ignore-pattern: # pattern of filenames which are ignored while protocol processing (e.g. for files which were spawned from another file).
      tag-protocol-processed: # if to tag files which were contained in a successful protocol
```

### Adding additional target

For adding additional destinations the according outgoing Kafka binding needs to be added as following:

```yaml

spring:
  cloud:
    stream:
      bindings:
        example-out:
          destination: swim-example-local
```

The binding key (in the above example `example-out`) can then be used as use case destination.

## Rerouting

To allow a preceding system to change the routing behaviour there is the property `swim.dispatch-action-tag-key`, which supports following values:

- `dispatch` (default if action tag not set): Default routing behaviour
- `reroute`: Reroute a message to another use case. Target use case is resolved via `swim.dispatch-action-destination-tag-key`
- `delete`, `ignore`: File is marked as finished without further processing
