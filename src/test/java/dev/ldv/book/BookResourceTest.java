package dev.ldv.book;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class BookResourceTest {

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
                .when()
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
    void shouldGet404ForUnknownBook() {
        given()
                .get("/books/999")
                .then()
                .statusCode(404);
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
                    .when()
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
}
