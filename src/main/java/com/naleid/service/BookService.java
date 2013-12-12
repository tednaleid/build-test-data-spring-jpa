package com.naleid.service;

import com.naleid.entity.Book;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class BookService {
    @PersistenceContext
    EntityManager entityManager;

    Book orderBookById(Long id) {
        return entityManager.find(Book.class, id);
    }

//    Book orderBookByTitle(String title) {
//
//    }
//
//    Book orderBookByAuthor(Author author) {
//
//    }
}
