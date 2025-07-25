# SWIM DMS-Service

SWIM service for transferring files into the "eAkte" (internal German name for a record and document management system, in this repository further referred to as DMS) when notified by [dispatch-service](../dispatch-service) via Apache Kafka.
Based on [handler-core](../handler-core).

## Architecture

```mermaid
flowchart LR
    Kafka --> DMS-Service
    DMS-Service --> DMS-REST-EAI
    DMS-Service --> Kafka
```

For the DMS-REST-EAI the [`refarch-dms-integration-fabasoft-rest-api` module](https://github.com/it-at-m/refarch/tree/main/refarch-integrations/refarch-dms-integration/refarch-dms-integration-fabasoft-rest-api) is used. 

### Error handling

Errors which occur during processing a file are transmitted via Kafka to the [dispatch-service](../dispatch-service) which handles them accordingly.

### DMS

Further documentation regarding the DMS can be found here (internal only):
- https://confluence.muenchen.de/display/KM53/REST-EAI-Schnittstelle
- https://dmsresteai-dev-dmsresteai.apps.capk.muenchen.de/swagger-ui/index.html

The different DMS resources (used in this service) follow following hierarchy (in syntax "english (german)"):

Fileplan (Aktenplan) → (multiple) Apentry (Aktenplaneintrag) → SubjectArea (Aktenplaneintrag (Betreffseinheit)) → File (Sachakte) → Procedure (Vorgang) → Incoming (Eingang) → ContentObject (Schriftstück)

## Development

- The dms-service is built with JDK21
- For local development and testing the dev docker-compose stack of the [dispatch-service](../dispatch-service) can be used.
    - Can be started with `docker compose up -d`
- The Spring profile `local` is preconfigured for using the stack
    - Activate it either manually or by using the provided run configuration
    - Additionally, the dms credentials need to be configured in the [`application-local.yml`](./src/main/resources/application-local.yml). See [Configuration](#configuration) for reference.
- After starting the application, file processing can be triggered via Kafka in one of the following ways:
    - via [dispatch-service](../dispatch-service)
    - via [Kafka-UI](http://localhost:8089/)

## Configuration

```yaml
swim:
  # dms connection configuration
  dms:
    base-url:
    username:
    password:
  decode-german-chars-prefix: '#' # prefix for <use case>.decode-german-chars
  # metadata keys (default values)
  metadata-subject-prefix: "FdE_" # prefix to build subject from metadata file, see Metadata
  metadata-dms-target-key: "SWIM_DMS_Target" # key to use for resolving dms target type, see Type metadata_file
  metadata-user-inbox-coo-key: "PPK_COO" # key to use for resolving target user inbox, see Coo source metadata_file and Metadata
  metadata-user-inbox-user-key: "PPK_Username"
  metadata-group-inbox-coo-key: "GPK_COO" # key to use for resolving target group inbox, see Coo source metadata_file and Metadata
  metadata-group-inbox-user-key: "GPK_Username"
  metadata-incoming-coo-key: "VG_COO" # key to use for resolving target incoming, see Coo source metadata_file and Metadata
  metadata-incoming-user-key: "VG_Username"
  metadata-incoming-joboe-key: "VG_Joboe"
  metadata-incoming-jobposition-key: "VG_Jobposition"
  # use cases
  use-cases:
    - name: # required
      type: # required, see section "Type"
      coo-source:
        type: # required, see section "Coo source"
        target-coo: # for coo-source static
        filename-coo-pattern: # for coo-source filename
        filename-to-coo: # for coo-source filename_map
        filename-name-pattern: # for coo-source filename_name
      context:
        username: # user under which the DMS action is executed
        joboe: # used to resolve user role under which the DMS action is executed, default role if not defined
        jobposition: # used to resolve user role under which the DMS action is executed, default role if not defined
      incoming:
        incoming-name-pattern: # overwrite Incoming name via Regex pattern, if result is empty falls back to default filename without extension
        incoming-subject-pattern: # pattern for subject of new Incoming; if this is defined metadata-subject needs to be false
        metadata-subject: # enables Incoming subject be built from metadata file, default false
        reuse-incoming: # if already existing Incoming (based on name) should be reused, when existing only ContentObject is created inside
        verify-procedure-name-pattern: # verifies target procedure name matches this pattern, only applies to type procedure_incoming
      content_object:
        subject-pattern: # pattern for subject of new ContentObject, currently only works inside Inbox
        filename-overwrite-pattern: # overwrite ContentObject name via Regex pattern
      decode-german-chars: # if german special chars should be decoded, default false. See section "Decode german chars"
```

### Pattern

The `*-pattern`-fields require a specific syntax (inspired by the sed command and regex substitution).

See [Pattern](../handler-core/README.md#pattern).

### Type

The `type` attribute of a use case defines what type of resource is created in the DMS.

- `inbox_content_object`: Creates an ContentObject inside a given Inbox.
- `inbox_incoming`: Creates an Incoming (with a ContentObject) inside a given Inbox.
- `procedure_incoming`: Creates an Incoming (with a ContentObject) inside a given Procedure or the OU work queue of the user.
- `metadata_file`: Resolve target type via metadata file. See [Configuration](#configuration) `metadata-dms-target-key` and [Metadata file](#metadata-file).

### Coo source

The `coo-source.type` attribute of a use case defines how the target resource, under which the new resource is created, is resolved.

- `metadata_file`: The target coo and username are resolved via a separate metadata file, which is placed beside the original file in the S3. See [Metadata file](#metadata-file). If username is missing it will be taken from `context` section of the usecase. 
- `static`: The target coo is defined statically via the `target-coo` use case attribute.
- `filename`: The target coo is resolved via the Regex pattern under `filename-coo-pattern`.
- `filename_map`: The target coo is resolved via the Map defined under `filename-to-coo`, which consist of pairs of Regex pattern and static coo. The coo of the first matching (case-insensitive) pattern is used.
- `filename_name`: The target coo is resolved via DMS object name. The name extracted via `filename-name-pattern` is looked up in the DMS and if exactly one match is found that is used as parent. In most cases the pattern should end with `*` as wildcard (e.g. `/^([^.]+)/${1}*/`).
- `ou_work_queue`: The Incoming is created inside the OU work queue of `username`. Can only be used with type `procedure_incoming`.

#### Metadata file

The metadata file is used for following different functions:

- Resolution of coo source
    - The dms target can be resolved via metadata file, see [Coo source](#coo-source) `metadata_file`
    - A valid metadata file in this case requires either personal `PPK_` or group `GPK_` inbox values defined (empty values are ignored) for `type: inbox` or `VG_` values for incoming or coo work queue. See [Configuration](#configuration).
- Subject
  - Values starting with `FdE_` (default) could be set as subject (see [Configuration](#configuration) `metadata-subject: true` and `metadata-subject-prefix`).
  - The below example would lead to a subject `Example Value 1 (ExampleKey1)\nExample Value 2 (ExampleKey2)`.
- Target type
  - The target resource type is resolved via metadata file. See [Configuration](#configuration) `metadata-dms-target-key` and [Type](#type) `metadata_file`.
  - Allowed values in metadata file are all use case [Types](#type) except `metadata_file` (e.g. `inbox_content_object`).

If a metadata file is required but missing or is invalid (syntax, value combination, ...) an Exception is thrown, which is handled by the [error-handling](#error-handling).

```json
{
  "Document": {
    "IndexFields": [
      {
        "Name": "SWIM_DMS_Target",
        "Value": ""
      },
      {
        "Name": "PPK_COO",
        "Value": ""
      },
      {
        "Name": "PPK_Username",
        "Value": ""
      },
      {
        "Name": "GPK_COO",
        "Value": ""
      },
      {
        "Name": "GPK_Username",
        "Value": ""
      },
      {
        "Name": "VG_COO",
        "Value": ""
      },
      {
        "Name": "VG_Username",
        "Value": ""
      },
      {
        "Name": "VG_Joboe",
        "Value": ""
      },
      {
        "Name": "VG_Jobposition",
        "Value": ""
      },
      {
        "Name": "FdE_ExampleKey1",
        "Value": "Example Value 1"
      },
      {
        "Name": "FdE_ExampleKey2",
        "Value": "Example Value 2"
      }
    ]
  }
}
```

### Decode german chars

Some german special chars/umlauts (e.g. üß) can lead to problems in different programms (e.g. Word barcodes).
For this a simple encoding was introduced which replaces the configured prefix (see `swim.decode-german-chars-prefix`)
joined with the simple form of a character to the special char. Needs to be enabled per use case via `decode-german-chars`.

- `#a` -> `ä`
- `#o` -> `ö`
- `#u` -> `ü`
- `#s` -> `ß`
- `#A` -> `Ä`
- `#O` -> `Ö`
- `#U` -> `Ü`
