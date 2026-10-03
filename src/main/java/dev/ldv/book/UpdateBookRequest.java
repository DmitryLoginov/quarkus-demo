package dev.ldv.book;

import jakarta.validation.constraints.*;

public record UpdateBookRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 200) String author,
        @NotNull @Min(1) @Max(2100) Integer publicationYear) {
}
