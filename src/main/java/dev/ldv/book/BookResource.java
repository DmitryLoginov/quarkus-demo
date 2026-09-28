package dev.ldv.book;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;
import java.util.Optional;

@Path("/books")
@Slf4j
public class BookResource {

    private final BookService bookService;

    public BookResource(BookService bookService) {
        this.bookService = bookService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<BookResponse> create(CreateBookRequest createBookRequest) {
        log.debug("CreateBookRequest: {}", createBookRequest);

        BookResponse bookResponse = bookService.createBook(createBookRequest);
        log.debug("BookResponse: {}", bookResponse);

        URI location = UriBuilder.fromPath("/books")
                .path("{id}")
                .build(bookResponse.id());
        log.debug("Location: {}", location);

        return RestResponse.ResponseBuilder.create(RestResponse.Status.CREATED, bookResponse)
                .location(location)
                .build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<BookResponse> getSingleBook(@RestPath Long id) {
        log.debug("Book id: {}", id);

        Optional<BookResponse> maybeBookResponse = bookService.getBookById(id);

        if (maybeBookResponse.isPresent()) {
            return RestResponse.ResponseBuilder.create(RestResponse.Status.OK, maybeBookResponse.get())
                    .build();
        }

        return RestResponse.notFound();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BookPageResponse getPage() {
        return bookService.getPageOfBooks();
    }
}
