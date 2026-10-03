package dev.ldv.book;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Slf4j
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest createBookRequest) {
        Book book = fromRequest(createBookRequest);
        bookRepository.persistAndFlush(book);
        log.debug("Book created: {}", book);

        return toResponse(book);
    }

    @Transactional
    public Optional<BookResponse> getBookById(Long id) {
        Optional<Book> book = bookRepository.findByIdOptional(id);

        if (book.isEmpty()) {
            log.debug("Book with id {} not found", id);
            return Optional.empty();
        }

        log.debug("Book found: {}", book.get());
        return Optional.of(toResponse(book.get()));
    }

    @Transactional
    public BookPageResponse getPageOfBooks() {
        long count = bookRepository.count();
        List<Book> sortedPage = bookRepository.findAll(Sort.by("id"))
                .page(Page.ofSize(20))
                .list();
        log.debug("Books total: {}", count);

        return new BookPageResponse(sortedPage.stream()
                .map(this::toResponse)
                .toList(),
                0,
                20,
                count);
    }

    @Transactional
    public Optional<BookResponse> updateBook(Long id, UpdateBookRequest updateBookRequest) {
        Optional<Book> maybeBook = bookRepository.findByIdOptional(id);

        if (maybeBook.isEmpty()) {
            log.debug("Book with id {} not found", id);
            return Optional.empty();
        }

        Book book = maybeBook.get();

        log.debug("Book found: {}", book);

        updateBookFields(book, updateBookRequest);
        log.debug("Book updated: {}", book);

        return Optional.of(toResponse(book));
    }

    @Transactional
    public boolean deleteBook(Long id) {
        if (bookRepository.findByIdOptional(id).isEmpty()) {
            log.debug("Book with id {} not found", id);
            return false;
        }

        bookRepository.deleteById(id);
        return true;
    }

    private Book fromRequest(CreateBookRequest createBookRequest) {
        Book book = new Book();

        book.setTitle(createBookRequest.title());
        book.setAuthor(createBookRequest.author());
        book.setPublicationYear(createBookRequest.publicationYear());

        return book;
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublicationYear()
        );
    }

    private void updateBookFields(Book book, UpdateBookRequest updateBookRequest) {
        book.setTitle(updateBookRequest.title());
        book.setAuthor(updateBookRequest.author());
        book.setPublicationYear(updateBookRequest.publicationYear());
    }
}
