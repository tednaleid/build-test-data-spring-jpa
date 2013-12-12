package com.naleid.entity;

import java.util.Locale;

public enum Language {

    ENU("English", Locale.US),
    ESP("Spanish", Locale.forLanguageTag("es")),
    ENC("English-Canadian", Locale.CANADA),
    FRC("French-Canadian", Locale.CANADA_FRENCH);

    private String languageName;
    private Locale locale;

    Language(String languageName,Locale locale) {
        this.languageName = languageName;
        this.locale=locale;
    }

    public String getLanguageName() {
        return this.languageName;
    }

    public Locale getLocale() {
        return locale;
    }
}
