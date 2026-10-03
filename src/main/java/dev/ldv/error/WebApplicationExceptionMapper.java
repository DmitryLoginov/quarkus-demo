package dev.ldv.error;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (exception.getResponse().getStatus() == 400
                && exception.getCause() != null && exception.getCause() instanceof JsonParseException) {
            log.warn("400 {}", exception.getMessage(), exception);

            ApiError apiError = new ApiError(ApiError.BAD_REQUEST,
                    "JSON body is invalid",
                    ApiError.EMPTY_VIOLATIONS);
            log.debug(apiError.toString());

            return Response.status(Response.Status.BAD_REQUEST).entity(apiError)
                    .build();
        }

        return exception.getResponse();
    }
}
