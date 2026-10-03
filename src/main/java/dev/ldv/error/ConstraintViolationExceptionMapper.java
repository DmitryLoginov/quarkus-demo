package dev.ldv.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Provider
@Slf4j
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        log.warn("400 {}", exception.getMessage(), exception);

        List<ApiError.Violation> violations = new ArrayList<>();

        exception.getConstraintViolations().forEach(constraintViolation -> {
            Path propertyFullName = constraintViolation.getPropertyPath();
            String propertyName = getLastNodeName(propertyFullName);
            String errorMessage = constraintViolation.getMessage();

            violations.add(new ApiError.Violation(propertyName, errorMessage));
        });

        if (violations.stream().anyMatch(violation -> violation.field().equals("id"))) {
            ApiError apiError = new ApiError(ApiError.BAD_REQUEST,
                    "Bad request: invalid id",
                    violations.toArray(new ApiError.Violation[0]));
            log.debug(apiError.toString());

            return Response.status(Response.Status.BAD_REQUEST).entity(apiError)
                    .build();
        }

        ApiError apiError = new ApiError(ApiError.VALIDATION_ERROR,
                "Validation error: body entity has constraint violations",
                violations.toArray(new ApiError.Violation[0]));
        log.debug(apiError.toString());

        return Response.status(Response.Status.BAD_REQUEST).entity(apiError)
                .build();
    }

    private String getLastNodeName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }
}
