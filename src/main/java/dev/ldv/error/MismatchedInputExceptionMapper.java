package dev.ldv.error;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Provider
@Slf4j
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    @Override
    public Response toResponse(MismatchedInputException exception) {
        log.warn("400 {}", exception.getMessage(), exception);

        ApiError apiError = new ApiError(ApiError.BAD_REQUEST,
                "Validation error: request body is invalid",
                ApiError.EMPTY_VIOLATIONS);
        log.debug(apiError.toString());

        return Response.status(Response.Status.BAD_REQUEST).entity(apiError)
                .build();
    }
}
