package dev.ldv.error;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class InvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {

    @Override
    public Response toResponse(InvalidFormatException exception) {
        log.warn("400 {}", exception.getMessage(), exception);

        ApiError apiError = new ApiError(ApiError.BAD_REQUEST,
                "Validation error: some of the fields have invalid format",
                ApiError.EMPTY_VIOLATIONS);
        log.debug(apiError.toString());

        return Response.status(Response.Status.BAD_REQUEST).entity(apiError)
                .build();
    }
}
