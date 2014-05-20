package com.naleid.service

import com.naleid.builder.BookBuilder
import com.naleid.builder.EntityBuilder
import com.naleid.builder.EntityBuilderLocator
import com.naleid.entity.Author
import com.naleid.entity.Book
import com.naleid.entity.Language
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

    @Test
    public void builtBooksCanBePassedProperties() {
        def properties = [
                title: "The Code Book",
                description: "a history of cryptography",
                language: Language.ENC
        ]
        Book book = bookBuilder.build(properties)
        assert entityManager.contains(book)
        properties.each {k, v -> assert book[k] == v }
    }

    @Test
    public void anonymousInnerClassBuildingWorks() {
        EntityBuilder builder = new EntityBuilder<Book>(entityManager, entityBuilderLocator) {
            @Override
            Map getDefaultProperties() {
                [title: "A Brief History of Time"]
            }
        }

        Book book = builder.build([description: "stephen hawking"])

        Book foundBook = entityManager.find(Book, book.id)
        assert foundBook.title == "A Brief History of Time"
        assert foundBook.description == "stephen hawking"
    }


    @Test
    public void associatedClassesAreBuiltWhenRequired() {
        EntityBuilder builder = new EntityBuilder<Book>(entityManager, entityBuilderLocator) {
            @Override
            Map getDefaultAssociations() {
                [author: this.determineBuilder(Author.class)]
            }
        }

        Book book = builder.build()
        assert entityManager.contains(book)
        assert book.author != null

        Author author = entityManager.find(Author.class, book.author.id)
        assert author != null
    }

    @Test
    public void determineBuilderFindsAutoCreatedEntityBuildersWithoutConcreteClasses() {
        // if you already have another builder in hand, you can just call builder.determineBuilder(clazz) instead of this
        EntityBuilder<Author> authorBuilder = EntityBuilder.determineBuilder(Author.class, entityManager, entityBuilderLocator)

        Author author = authorBuilder.build()

        assert author != null
        assert author.id != null
    }
}
