package dev.ldv.book;

import dev.ldv.error.ApiError;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

@Path("/books")
@Slf4j
public class BookResource {

    private final BookService bookService;

    public BookResource(BookService bookService) {
        this.bookService = bookService;
    }

    @ServerExceptionMapper
    public RestResponse<ApiError> mapNotFoundException(NotFoundException exception) {
        if (exception.getCause() != null && exception.getCause() instanceof NumberFormatException cause) {
            log.warn("400 {}", cause.getMessage(), cause);

            ApiError apiError = new ApiError(ApiError.BAD_REQUEST,
                    "Bad request: invalid id",
                    new ApiError.Violation[]{new ApiError.Violation("id", "must be a number")});
            log.debug(apiError.toString());

            return RestResponse.ResponseBuilder.create(Response.Status.BAD_REQUEST, apiError)
                    .build();
        }

        log.warn("404 {}", exception.getMessage(), exception);

        ApiError apiError = new ApiError(ApiError.NOT_FOUND,
                exception.getMessage(),
                ApiError.EMPTY_VIOLATIONS);
        log.debug(apiError.toString());

        return RestResponse.ResponseBuilder.create(Response.Status.NOT_FOUND, apiError)
                .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<BookResponse> create(@Valid @NotNull CreateBookRequest createBookRequest) {
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
    public RestResponse<BookResponse> getSingleBook(@RestPath @Positive Long id) {
        log.debug("Book id: {}", id);

        Optional<BookResponse> maybeBookResponse = bookService.getBookById(id);

        if (maybeBookResponse.isEmpty()) {
            throw new NotFoundException("Book with id " + id + " not found");
        }

        return RestResponse.ResponseBuilder.create(RestResponse.Status.OK, maybeBookResponse.get())
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BookPageResponse getPage() {
        return bookService.getPageOfBooks();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<BookResponse> update(@RestPath @Positive Long id,
                                             @Valid @NotNull UpdateBookRequest updateBookRequest) {
        log.debug("UpdateBookRequest: {}", updateBookRequest);
        log.debug("Book id: {}", id);

        Optional<BookResponse> maybeBookResponse = bookService.updateBook(id, updateBookRequest);

        if (maybeBookResponse.isEmpty()) {
            throw new NotFoundException("Book with id " + id + " not found");
        }

        return RestResponse.ResponseBuilder.create(RestResponse.Status.OK, maybeBookResponse.get())
                .build();
    }

    @DELETE
    @Path("/{id}")
    public RestResponse<Void> deleteBook(@RestPath @Positive Long id) {
        log.debug("Book id: {}", id);

        boolean result = bookService.deleteBook(id);

        if (result) {
            return RestResponse.noContent();
        } else {
            throw new NotFoundException("Book with id " + id + " not found");
        }
    }
}
