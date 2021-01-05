package com.rwtema.denseores.utils;

import java.util.function.Supplier;

import static com.rwtema.denseores.DenseOresMod.wrap;

public final class Validate {
    private static final String DEFAULT_IS_NULL_EX_MESSAGE = "The validated object is null";
    private Validate(){}

    public static void isTrue(boolean value, String error) {
        isTrue(value, () -> error);
    }

    public static void isTrue(boolean value, Supplier<String> strSupplier) {
        isTrueFlex(value, () -> new IllegalStateException(strSupplier.get()));
    }

    public static void isTrueFlex(boolean value, Supplier<RuntimeException> exSupplier) {
        if (!value) {
            throw wrap(exSupplier.get());
        }
    }

    public static void isFalse(boolean value, String error) {
        isFalse(value, () -> error);
    }

    public static void isFalse(boolean value, Supplier<String> strSupplier) {
        isFalseFlex(value, () -> new IllegalStateException(strSupplier.get()));
    }

    public static void isFalseFlex(boolean value, Supplier<RuntimeException> exSupplier) {
        if (value) {
            throw wrap(exSupplier.get());
        }
    }


    public static <T> T notNull(T value) {
        return notNull(value, DEFAULT_IS_NULL_EX_MESSAGE);

    }
    public static <T> T notNull(T value, String error) {
        return notNull(value, () -> error);
    }

    public static <T> T notNull(T value, Supplier<String> strSupplier) {
        return notNullFlex(value, () -> new NullPointerException(strSupplier.get()));
    }

    public static <T> T notNullFlex(T value, Supplier<RuntimeException> exSupplier) {
        if (value == null) {
            throw wrap(exSupplier.get());
        }
        return value;
    }
}
