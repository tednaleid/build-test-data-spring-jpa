package com.naleid.entity;

import org.springframework.data.annotation.Id;

import javax.persistence.GeneratedValue;

public class Author {
    @Id
    @GeneratedValue
    private long id;

}
