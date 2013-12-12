package com.naleid.builder;

public interface EntityBuilderLocator {
    public EntityBuilder<?> lookup(String builderName);
}
