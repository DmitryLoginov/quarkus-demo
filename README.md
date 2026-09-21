# Quarkus Demo Project

## Running the application

```bash
.\mvnw quarkus:dev
.\mvnw.cmd quarkus:dev
```

## Testing

```bash
.\mvnw test
.\mvnw.cmd test
```

## Using
* Java 21
* `io.quarkus.platform:quarkus-bom:3.39.3` for dependency management
* `io.quarkus.platform:quarkus-maven-plugin:3.39.3` for packaging the application and providing development mode (live reload)

## Live Reload

Live Reload allow us to see changes immediately after editing Java or resource files just refreshing browser page, no application restart needed.

## Test Endpoint

`GET /api/info`

Response:

```json
{
  "name": "book-catalog",
  "framework": "Quarkus"
}
```

## Components

* `InfoResource` - HTTP-layer
* `InfoService` - business logic
* `CatalogConfig` - configuration properties

Invocation chain:

`InfoResource` -> `InfoService` -> `CatalogConfig`

## Profiles

* dev - profile for development mode. `InfoResource` returns *book-catalog* for `name`
* test - profile for testing. `InfoResource` returns *book-catalog-test* for `name`

## Database

Application is using Dev Services with PostgreSQL (`docker.io/library/postgres:18`).
Dev Services automatically starts a PostgreSQL server in Docker for dev purposes and when running tests.
The application is configured automatically.

Application is using volumes **in dev mode** to preserve database data and reuse it after an application restart:

`%dev.quarkus.datasource.devservices.volumes."../volumes/quarkus-demo"=/var/lib/postgresql`

### Flyway

Application is using Flyway as database migration tool.

As application uses volumes for dev mode, Flyway migrations apply only once at startup.

However, application creates new clean temporary database when running tests.
Test containers are not reusable.

### Hibernate

Application is using Hibernate for mapping objects to database.