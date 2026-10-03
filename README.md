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

### Components

* `InfoResource` - HTTP-layer
* `InfoService` - business logic
* `CatalogConfig` - configuration properties

Invocation chain:

`InfoResource` -> `InfoService` -> `CatalogConfig`

### Profiles

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

As application uses volumes for dev mode, Flyway checks history of migrations and applies new migrations at every start.

However, application creates new clean temporary database when running tests.
Test containers are not reusable.

### Hibernate

Application is using Hibernate for mapping objects to database.
Schema management strategy is *validate*, which means that Hibernate doesn't change or create schema, but only validates existing schema.

## Books Endpoints

`GET /books/{id}`:

Path params:
* `id` - book id

Response example (200):

```json
{
  "id": 1,
  "title": "Title",
  "author": "Author",
  "publicationYear": 2000
}
```

Endpoint returns 404 when book with given id is not found.

`POST /books`:

Request body example:

```json
{
  "title": "Title",
  "author": "Author",
  "publicationYear": 2000
}
```

Response example (201):

```json
{
  "id": 1,
  "title": "Title",
  "author": "Author",
  "publicationYear": 2000
}
```

Successful response also has `Location` header with relative URI of new book.

In case of invalid field values server returns 400 with list of violations.


`GET /books`:

Response example (200):

```json
{
  "items": [
    {
      "id": 1,
      "title": "Title",
      "author": "Author",
      "publicationYear": 2000
    }
  ],
  "page": 0,
  "size": 20,
  "total": 1
}
```

`PUT /books/{id}`

Path params:
* `id` - book id

* Request body example:

```json
{
  "title": "Title",
  "author": "Author",
  "publicationYear": 2000
}
```

Response example (200):

```json
{
  "id": 1,
  "title": "Title",
  "author": "Author",
  "publicationYear": 2000
}
```

In case of invalid field values server returns 400 with list of violations.

If target book was not found, server returns 404.

`DELETE /books/{id}`

Path params:
* `id` - book id

In case of successful removal server returns 204 without body.

#### Common error format

Error examples:

```json
{
  "code": "NOT_FOUND",
  "message": "Book with id 999 not found",
  "violations": []
}
```
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation error: body entity has constraint violations",
  "violations": [
    {
      "field": "publicationYear",
      "message": "must be greater than zero"
    }
  ]
}
```

### Components

* `BookResource` - HTTP layer
* `BookService` - business logic and transactions
* `BookRepository` - database layer

Invocation chain:

`BookResource` -> `BookService` -> `BookRepository`