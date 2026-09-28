package dev.ldv.book;

import java.util.List;

public record BookPageResponse(List<BookResponse> items, int page, int size, long total) {
}
