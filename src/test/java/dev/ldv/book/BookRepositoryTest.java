package dev.ldv.book;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class BookRepositoryTest {

    @Inject
    private BookRepository bookRepository;

    @Test
    @TestTransaction
    void shouldSaveAndGetBook() {
        Book bookToPersist = new Book();
        bookToPersist.setTitle("Book");
        bookToPersist.setAuthor("Author");
        bookToPersist.setPublicationYear(2018);

        bookRepository.persistAndFlush(bookToPersist);

        Long id = bookToPersist.getId();

        EntityManager em = bookRepository.getEntityManager();
        em.clear();

        Book foundBook = bookRepository.findById(id);

        assertEquals(bookToPersist.getTitle(), foundBook.getTitle());
        assertEquals(bookToPersist.getAuthor(), foundBook.getAuthor());
        assertEquals(bookToPersist.getPublicationYear(), foundBook.getPublicationYear());
    }
}
