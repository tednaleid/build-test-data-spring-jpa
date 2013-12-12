package com.naleid.entity;

import javax.persistence.*;

@Entity
public class Book {

    @Id
    @GeneratedValue
    private long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Language language;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }
}
