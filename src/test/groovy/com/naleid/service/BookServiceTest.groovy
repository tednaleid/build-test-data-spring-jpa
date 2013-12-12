package com.naleid.service

import com.naleid.builder.BookBuilder
import com.naleid.builder.EntityBuilder
import com.naleid.builder.EntityBuilderLocator
import com.naleid.entity.Book
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/testApplicationContext.xml")
@Transactional
class BookServiceTest {

    @Autowired
    EntityBuilderLocator entityBuilderLocator

    @Autowired
    BookBuilder bookBuilder

    @Autowired
    BookService bookService

    @PersistenceContext
    EntityManager entityManager

    @Test
    public void orderBookById() {
        Book book = bookBuilder.build()

        assert entityManager.find(Book, book.id) != null

        Long bookId = book.id

        assert bookService.orderBookById(bookId).id == bookId
    }
}
