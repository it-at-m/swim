# SWIM matching service

Service for managing recipient matching data for the SWIM (stadtweites Inputmanagement).

## Data

Following data is collected and provided:
- DMS user inboxes which are enriched with the correlated user ldap data and put into the SWIM database
- DMS group inboxes which are directly put into the SWIM database

## Architecture

```mermaid
flowchart LR
    dms[DMS]
    u([User])
    s[SWIM matching service]
    l[LDAP]
    db[(SWIM db)]
    dms --- u
    u --> s
    s --> l
    s --> db
```

## Development

- The swim-matching-service is built with JDK21
- For local development and testing a dev docker-compose stack is provided in `./stack`
  - Can be started with `docker compose up -d`
- The Spring profile `local` is preconfigured for using the stack
  - Activate it either manually or by using the provided run configuration
- After starting the application the import can be triggered via the [Swagger-UI](http://localhost:39146/swagger-ui/index.html)
  - The default login is `user` with password `user`
