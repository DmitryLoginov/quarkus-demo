package dev.ldv.book;

import dev.ldv.error.ApiError;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class BookResourceTest {

    private static final String acceptableString = "a".repeat(199);
    private static final String stillAcceptableString = "a".repeat(200);
    private static final String notAcceptableString = "a".repeat(201);

    @Inject
    private BookRepository bookRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        bookRepository.deleteAll();
    }

    @AfterEach
    @Transactional
    void cleanUp() {
        bookRepository.deleteAll();
    }

    @Test
    void shouldCreateAndGetBook() {
        CreateBookRequest request = new CreateBookRequest("test-title",
                "test-author", 2000);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().response();

        BookResponse bookPostResponse = postResponse.body().as(BookResponse.class);
        assertNotNull(bookPostResponse.id());
        assertEquals(request.title(), bookPostResponse.title());
        assertEquals(request.author(), bookPostResponse.author());
        assertEquals(request.publicationYear(), bookPostResponse.publicationYear());

        String location = postResponse.getHeader("Location");

        Response getResponse = given()
                .get(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        BookResponse getBookResponse = getResponse.body().as(BookResponse.class);
        assertEquals(bookPostResponse, getBookResponse);

        Response getPageResponse = given()
                .get("/books")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        BookPageResponse bookPageResponse = getPageResponse.body().as(BookPageResponse.class);
        assertEquals(1, bookPageResponse.items().size());
        assertEquals(bookPostResponse, bookPageResponse.items().getFirst());
    }

    @Test
    void shouldGet404WhenTryingToGetUnknownBook() {
        ApiError getApiError = given()
                .get("/books/999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().response().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, getApiError.code());
        assertEquals("Book with id 999 not found", getApiError.message());
        assertNotNull(getApiError.violations());
        assertEquals(0, getApiError.violations().length);
    }

    @Test
    void shouldGetNoBooksWhenDatabaseIsEmpty() {
        Response getPageResponse = given()
                .get("/books")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        BookPageResponse bookPageResponse = getPageResponse.body().as(BookPageResponse.class);
        assertEquals(0, bookPageResponse.items().size());
        assertEquals(0, bookPageResponse.total());
        assertEquals(0, bookPageResponse.page());
        assertEquals(20, bookPageResponse.size());
    }

    @Test
    void shouldGetExactly20BooksWhenThereIsMoreThan20BooksInDatabase() {
        int total = 25;
        List<Long> expectedBookIds = new ArrayList<>();

        for (int i = 1; i <= total; i++) {
            CreateBookRequest request = new CreateBookRequest("test-title-" + i,
                    "test-author-" + i, 2000 + i);

            Response response = given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .post("/books")
                    .then()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .extract().response();

            BookResponse bookPostResponse = response.body().as(BookResponse.class);
            expectedBookIds.add(bookPostResponse.id());
        }

        Response getPageResponse = given()
                .get("/books")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();

        BookPageResponse bookPageResponse = getPageResponse.body().as(BookPageResponse.class);
        assertEquals(20, bookPageResponse.items().size());
        assertEquals(total, bookPageResponse.total());
        assertEquals(0, bookPageResponse.page());
        assertEquals(20, bookPageResponse.size());

        List<Long> ids = bookPageResponse.items().stream()
                .map(BookResponse::id)
                .toList();

        assertIterableEquals(
                ids.stream()
                        .sorted()
                        .toList(),
                ids
        );
        assertIterableEquals(
                expectedBookIds.stream()
                        .sorted()
                        .limit(20)
                        .toList(),
                ids
        );
    }

    @ParameterizedTest
    @MethodSource("validUpdateRequests")
    void shouldUpdateBook(UpdateBookRequest updateRequest) {
        CreateBookRequest createRequest = new CreateBookRequest("test-title",
                "test-author", 2000);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .extract().response();

        BookResponse bookResponse = response.body().as(BookResponse.class);
        String location = response.getHeader("Location");

        BookResponse updateResponse = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(BookResponse.class);

        assertEquals(bookResponse.id(), updateResponse.id());
        assertEquals(updateRequest.title(), updateResponse.title());
        assertEquals(updateRequest.author(), updateResponse.author());
        assertEquals(updateRequest.publicationYear(), updateResponse.publicationYear());
    }

    static Stream<UpdateBookRequest> validUpdateRequests() {
        return Stream.of(
                new UpdateBookRequest("updated title", "test-author", 2000),
                new UpdateBookRequest("test-title", "updated author", 2000),
                new UpdateBookRequest("test-title", "test-author", 2020)
        );
    }

    @Test
    void shouldReturn404WhenBookForUpdateIsNotFound() {
        UpdateBookRequest updateRequest = new UpdateBookRequest("test-title", "test-author",
                2000);

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put("/books/999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().response().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, putApiError.code());
        assertEquals("Book with id 999 not found", putApiError.message());
        assertNotNull(putApiError.violations());
        assertEquals(0, putApiError.violations().length);

        ApiError getApiError = given()
                .get("/books/999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().response().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, getApiError.code());
        assertEquals("Book with id 999 not found", getApiError.message());
        assertNotNull(getApiError.violations());
        assertEquals(0, getApiError.violations().length);
    }

    @Test
    void shouldDeleteBook() {
        CreateBookRequest createRequest = new CreateBookRequest("test-title",
                "test-author", 2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .extract().header("Location");

        String body = given()
                .delete(location)
                .then()
                .statusCode(204)
                .extract().body().asString();
        assertTrue(body == null || body.isEmpty());

        given()
                .get(location)
                .then()
                .statusCode(404);

        given()
                .delete(location)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldReturn404WhenTryingToDeleteUnknownBook() {
        ApiError deleteApiError = given()
                .delete("/books/999")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, deleteApiError.code());
        assertEquals("Book with id 999 not found", deleteApiError.message());
        assertNotNull(deleteApiError.violations());
        assertEquals(0, deleteApiError.violations().length);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidCreateRequests")
    void shouldReturn400WhenCreateBookRequestIsInvalid(String field,
                                                       String description,
                                                       CreateBookRequest createRequest) {
        ApiError postApiError = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, postApiError.code());
        assertEquals("Validation error: body entity has constraint violations", postApiError.message());
        assertNotEquals(0, postApiError.violations().length);
        assertTrue(Arrays.stream(postApiError.violations())
                .anyMatch(violation -> violation.field().equals(field)));
        assertNotNull(postApiError.violations()[0].message());
        assertFalse(postApiError.violations()[0].message().isBlank());

        BookPageResponse bookPageResponse = given()
                .get("/books")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(BookPageResponse.class);

        assertEquals(0, bookPageResponse.items().size());
        assertEquals(0, bookPageResponse.total());
    }

    static Stream<Arguments> invalidCreateRequests() {
        return Stream.of(
                Arguments.of("title", "title = null",
                        new CreateBookRequest(null, "author", 2020)),
                Arguments.of("title", "title = empty",
                        new CreateBookRequest("", "author", 2020)),
                Arguments.of("title", "title = whitespace",
                        new CreateBookRequest(" ", "author", 2020)),
                Arguments.of("title", "title is too long",
                        new CreateBookRequest(notAcceptableString, "author", 2020)),
                Arguments.of("author", "author = null",
                        new CreateBookRequest("title", null, 2020)),
                Arguments.of("author", "author = empty",
                        new CreateBookRequest("title", "", 2020)),
                Arguments.of("author", "author = whitespace",
                        new CreateBookRequest("title", " ", 2020)),
                Arguments.of("author", "author is too long",
                        new CreateBookRequest("title", notAcceptableString, 2020)),
                Arguments.of("publicationYear", "publicationYear = null",
                        new CreateBookRequest("title", "author", null)),
                Arguments.of("publicationYear", "publicationYear < 1",
                        new CreateBookRequest("title", "author", 0)),
                Arguments.of("publicationYear", "publicationYear > 2100",
                        new CreateBookRequest("title", "author", 2101))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 1000, 2099, 2100})
    void shouldCreateBookWithValidPublicationYear(int publicationYear) {
        CreateBookRequest createRequest = new CreateBookRequest("title", "author", publicationYear);

        given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTitleAndAuthorCreateRequests")
    void shouldCreateBookWithAcceptableTitleLength(String description,
                                                   CreateBookRequest createRequest) {
        given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON);
    }

    static Stream<Arguments> validTitleAndAuthorCreateRequests() {
        return Stream.of(
                Arguments.of("title of 199 chars",
                        new CreateBookRequest(acceptableString, "author", 2020)),
                Arguments.of("title of 200 chars",
                        new CreateBookRequest(stillAcceptableString, "author", 2020)),
                Arguments.of("author of 199 chars",
                        new CreateBookRequest("title", acceptableString, 2020)),
                Arguments.of("author of 200 chars",
                        new CreateBookRequest("title", stillAcceptableString, 2020))
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidUpdateRequests")
    void shouldReturn400WhenUpdateBookRequestIsInvalid(String field,
                                                       String description,
                                                       UpdateBookRequest updateRequest) {
        CreateBookRequest createRequest = new CreateBookRequest("title", "author", 2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .extract().header("Location");

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put(location)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, putApiError.code());
        assertEquals("Validation error: body entity has constraint violations", putApiError.message());
        assertNotEquals(0, putApiError.violations().length);
        assertTrue(Arrays.stream(putApiError.violations())
                .anyMatch(violation -> violation.field().equals(field)));
        assertNotNull(putApiError.violations()[0].message());
        assertFalse(putApiError.violations()[0].message().isBlank());

        BookResponse bookResponse = given()
                .contentType(ContentType.JSON)
                .get(location)
                .then()
                .extract().body().as(BookResponse.class);

        assertEquals(createRequest.title(), bookResponse.title());
        assertEquals(createRequest.author(), bookResponse.author());
        assertEquals(createRequest.publicationYear(), bookResponse.publicationYear());

        BookPageResponse bookPageResponse = given()
                .get("/books")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(BookPageResponse.class);

        // exactly one book
        assertEquals(1, bookPageResponse.items().size());
        assertEquals(1, bookPageResponse.total());
    }

    static Stream<Arguments> invalidUpdateRequests() {
        return Stream.of(
                Arguments.of("title", "title = null",
                        new UpdateBookRequest(null, "author", 2020)),
                Arguments.of("title", "title = empty",
                        new UpdateBookRequest("", "author", 2020)),
                Arguments.of("title", "title = whitespace",
                        new UpdateBookRequest(" ", "author", 2020)),
                Arguments.of("title", "title is too long",
                        new UpdateBookRequest(notAcceptableString, "author", 2020)),
                Arguments.of("author", "author = null",
                        new UpdateBookRequest("title", null, 2020)),
                Arguments.of("author", "author = empty",
                        new UpdateBookRequest("title", "", 2020)),
                Arguments.of("author", "author = whitespace",
                        new UpdateBookRequest("title", " ", 2020)),
                Arguments.of("author", "author is too long",
                        new UpdateBookRequest("title", notAcceptableString, 2020)),
                Arguments.of("publicationYear", "publicationYear = null",
                        new UpdateBookRequest("title", "author", null)),
                Arguments.of("publicationYear", "publicationYear < 1",
                        new UpdateBookRequest("title", "author", 0)),
                Arguments.of("publicationYear", "publicationYear > 2100",
                        new UpdateBookRequest("title", "author", 2101))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 1000, 2099, 2100})
    void shouldUpdateBookWithValidPublicationYear(int publicationYear) {
        CreateBookRequest createRequest = new CreateBookRequest("title", "author", 2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .extract().header("Location");

        UpdateBookRequest updateRequest = new UpdateBookRequest("title", "author", publicationYear);

        BookResponse updateResponse = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(BookResponse.class);

        assertEquals(updateRequest.title(), updateResponse.title());
        assertEquals(updateRequest.author(), updateResponse.author());
        assertEquals(updateRequest.publicationYear(), updateResponse.publicationYear());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validTitleAndAuthorUpdateRequests")
    void shouldUpdateBookWithAcceptableTitleLength(String description,
                                                   UpdateBookRequest updateRequest) {
        CreateBookRequest createRequest = new CreateBookRequest("title", "author", 2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .extract().header("Location");

        BookResponse updateResponse = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().body().as(BookResponse.class);

        assertEquals(updateRequest.title(), updateResponse.title());
        assertEquals(updateRequest.author(), updateResponse.author());
        assertEquals(updateRequest.publicationYear(), updateResponse.publicationYear());
    }

    static Stream<Arguments> validTitleAndAuthorUpdateRequests() {
        return Stream.of(
                Arguments.of("title of 199 chars",
                        new UpdateBookRequest(acceptableString, "author", 2020)),
                Arguments.of("title of 200 chars",
                        new UpdateBookRequest(stillAcceptableString, "author", 2020)),
                Arguments.of("author of 199 chars",
                        new UpdateBookRequest("title", acceptableString, 2020)),
                Arguments.of("author of 200 chars",
                        new UpdateBookRequest("title", stillAcceptableString, 2020))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidPathParams")
    void shouldReturn400WhenPathParamForGETIsInvalid(Object id) {
        ApiError getApiError = given()
                .get("/books/" + id)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.BAD_REQUEST, getApiError.code());
        assertEquals("Bad request: invalid id", getApiError.message());
        assertNotEquals(0, getApiError.violations().length);
        assertTrue(Arrays.stream(getApiError.violations())
                .anyMatch(violation -> violation.field().equals("id")));
        assertNotNull(getApiError.violations()[0].message());
        assertFalse(getApiError.violations()[0].message().isBlank());
    }

    @ParameterizedTest
    @MethodSource("invalidPathParams")
    void shouldReturn400WhenPathParamForPUTIsInvalid(Object id) {
        UpdateBookRequest updateRequest = new UpdateBookRequest("title", "author", 2020);

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put("/books/" + id)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.BAD_REQUEST, putApiError.code());
        assertEquals("Bad request: invalid id", putApiError.message());
        assertNotEquals(0, putApiError.violations().length);
        assertTrue(Arrays.stream(putApiError.violations())
                .anyMatch(violation -> violation.field().equals("id")));
        assertNotNull(putApiError.violations()[0].message());
        assertFalse(putApiError.violations()[0].message().isBlank());
    }

    @ParameterizedTest
    @MethodSource("invalidPathParams")
    void shouldReturn400WhenPathParamForDELETEIsInvalid(Object id) {
        ApiError deleteApiError = given()
                .delete("/books/" + id)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.BAD_REQUEST, deleteApiError.code());
        assertEquals("Bad request: invalid id", deleteApiError.message());
        assertNotEquals(0, deleteApiError.violations().length);
        assertTrue(Arrays.stream(deleteApiError.violations())
                .anyMatch(violation -> violation.field().equals("id")));
        assertNotNull(deleteApiError.violations()[0].message());
        assertFalse(deleteApiError.violations()[0].message().isBlank());
    }

    static Stream<Object> invalidPathParams() {
        return Stream.of(-1, 0, "abc");
    }

    @ParameterizedTest
    @MethodSource("malformedJsonBodies")
    void shouldReturn400WhenPOSTBodyIsMalformed(String message, String body) {
        ApiError postApiError = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/books")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.BAD_REQUEST, postApiError.code());
        assertEquals(message, postApiError.message());
        assertNotNull(postApiError.violations());
        assertEquals(0, postApiError.violations().length);
    }

    @ParameterizedTest
    @MethodSource("malformedJsonBodies")
    void shouldReturn400WhenPUTBodyIsMalformed(String message, String body) {
        CreateBookRequest request = new CreateBookRequest("test-title", "test-author",
                2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().header("Location");

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(body)
                .put(location)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.BAD_REQUEST, putApiError.code());
        assertEquals(message, putApiError.message());
        assertNotNull(putApiError.violations());
        assertEquals(0, putApiError.violations().length);
    }

    static Stream<Arguments> malformedJsonBodies() {
        return Stream.of(
                Arguments.of("JSON body is invalid",
                        """
                                {
                                    "title": "title",
                                    "author": "author",
                                    "publicationYear": 2020,
                                """),
                Arguments.of("Validation error: some of the fields have invalid format",
                        """
                                {
                                    "title": "title",
                                    "author": "author",
                                    "publicationYear": "abc"
                                }
                                """),
                Arguments.of("Validation error: request body is invalid",
                        """
                                {
                                    "title": {
                                        "unknownField": "value"
                                    },
                                    "author": "author",
                                    "publicationYear": 2020
                                }
                                """)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null"})
    void shouldReturn400WhenPOSTBodyStringIsNullOrEmpty(String body) {
        ApiError postApiError = given()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/books")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, postApiError.code());
        assertEquals("Validation error: body entity has constraint violations", postApiError.message());
        assertNotNull(postApiError.violations());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "null"})
    void shouldReturn400WhenPUTBodyStringIsNullOrEmpty(String body) {
        CreateBookRequest request = new CreateBookRequest("test-title", "test-author",
                2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().header("Location");

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(body)
                .put(location)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, putApiError.code());
        assertEquals("Validation error: body entity has constraint violations", putApiError.message());
        assertNotNull(putApiError.violations());
    }

    @Test
    void shouldReturn400WhenPOSTBodyIsMissing() {
        ApiError postApiError = given()
                .contentType(ContentType.JSON)
                .post("/books")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, postApiError.code());
        assertEquals("Validation error: body entity has constraint violations", postApiError.message());
        assertNotNull(postApiError.violations());
    }

    @Test
    void shouldReturn400WhenPUTBodyIsMissing() {
        CreateBookRequest request = new CreateBookRequest("test-title", "test-author",
                2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().header("Location");

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .put(location)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, putApiError.code());
        assertEquals("Validation error: body entity has constraint violations", putApiError.message());
        assertNotNull(putApiError.violations());
    }

    @Test
    void createGetDeleteGetDeleteScenario() {
        CreateBookRequest createRequest =
                new CreateBookRequest("title", "author", 2000);

        Response postResponse = given()
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().response();
        String location = postResponse.header("Location");
        long id = Long.parseLong(location.substring(location.lastIndexOf("/") + 1));
        BookResponse newBookResponse = postResponse.body().as(BookResponse.class);

        assertEquals(id, newBookResponse.id());
        assertEquals(createRequest.title(), newBookResponse.title());
        assertEquals(createRequest.author(), newBookResponse.author());
        assertEquals(createRequest.publicationYear(), newBookResponse.publicationYear());

        UpdateBookRequest updateRequest =
                new UpdateBookRequest("new title", "new author", 2020);

        Response putResponse = given()
                .contentType(ContentType.JSON)
                .body(updateRequest)
                .put(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();
        BookResponse updatedBookResponse = putResponse.body().as(BookResponse.class);

        assertEquals(id, updatedBookResponse.id());
        assertEquals(updateRequest.title(), updatedBookResponse.title());
        assertEquals(updateRequest.author(), updatedBookResponse.author());
        assertEquals(updateRequest.publicationYear(), updatedBookResponse.publicationYear());

        Response getResponse = given()
                .get(location)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().response();
        BookResponse getBookResponse = getResponse.body().as(BookResponse.class);

        assertEquals(id, getBookResponse.id());
        assertEquals(updateRequest.title(), getBookResponse.title());
        assertEquals(updateRequest.author(), getBookResponse.author());
        assertEquals(updateRequest.publicationYear(), getBookResponse.publicationYear());

        given()
                .delete(location)
                .then()
                .statusCode(204)
                .extract().response();

        Response getAfterDeleteResponse = given()
                .get(location)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().response();
        ApiError getApiError = getAfterDeleteResponse.body().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, getApiError.code());

        Response repeatedDeleteCallResponse = given()
                .delete(location)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().response();
        ApiError deleteApiError = repeatedDeleteCallResponse.body().as(ApiError.class);

        assertEquals(ApiError.NOT_FOUND, deleteApiError.code());
    }

    @ParameterizedTest
    @MethodSource("jsonBodiesWithMissingFields")
    void shouldReturn400WhenMissingFieldsInPOSTBody(String missingField, String bodyWithMissingField) {
        ApiError postApiError = given()
                .contentType(ContentType.JSON)
                .body(bodyWithMissingField)
                .post("/books")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, postApiError.code());
        assertEquals("Validation error: body entity has constraint violations", postApiError.message());
        assertNotEquals(0, postApiError.violations().length);
        assertTrue(Arrays.stream(postApiError.violations())
                .anyMatch(violation -> violation.field().equals(missingField)));
        assertNotNull(postApiError.violations());
        assertEquals(1, postApiError.violations().length);
    }

    @ParameterizedTest
    @MethodSource("jsonBodiesWithMissingFields")
    void shouldReturn400WhenMissingFieldsInPUTBody(String missingField, String bodyWithMissingField) {
        CreateBookRequest request = new CreateBookRequest("test-title", "test-author",
                2000);

        String location = given()
                .contentType(ContentType.JSON)
                .body(request)
                .post("/books")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().header("Location");

        ApiError putApiError = given()
                .contentType(ContentType.JSON)
                .body(bodyWithMissingField)
                .put(location)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().body().as(ApiError.class);

        assertEquals(ApiError.VALIDATION_ERROR, putApiError.code());
        assertEquals("Validation error: body entity has constraint violations", putApiError.message());
        assertNotEquals(0, putApiError.violations().length);
        assertTrue(Arrays.stream(putApiError.violations())
                .anyMatch(violation -> violation.field().equals(missingField)));
        assertNotNull(putApiError.violations());
        assertEquals(1, putApiError.violations().length);
    }

    static Stream<Arguments> jsonBodiesWithMissingFields() {
        return Stream.of(
                Arguments.of("title",
                        """
                                {
                                    "author": "author",
                                    "publicationYear": 2020
                                }
                                """),
                Arguments.of("author",
                        """
                                {
                                    "title": "title",
                                    "publicationYear": 2020
                                }
                                """),
                Arguments.of("publicationYear",
                        """
                                {
                                    "title": "title",
                                    "author": "author"
                                }
                                """)
        );
    }
}
