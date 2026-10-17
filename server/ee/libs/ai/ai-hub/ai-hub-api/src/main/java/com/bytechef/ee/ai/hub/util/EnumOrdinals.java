/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * Resolves an int ordinal to its corresponding enum value with a bounds check that throws a typed
 * {@link IllegalStateException} for out-of-range values. Without this, a corrupt or future-version DB column would
 * surface as an {@link ArrayIndexOutOfBoundsException} from {@code Enum.values()[ordinal]} which is harder to diagnose
 * and lacks a useful message.
 *
 * <p>
 * <b>Intended for DB row mapping only.</b> The {@link IllegalStateException} contract reflects "the persisted state is
 * inconsistent with the current enum definition" — a server-side invariant violation, not a caller mistake. For
 * caller-supplied ordinals (request parameters, JSON payloads), validate at the boundary and throw
 * {@link IllegalArgumentException} per the conventional Java contract; do not pass user input through this helper.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EnumOrdinals {

    private EnumOrdinals() {
    }

    /**
     * Resolves {@code ordinal} to a value of {@code enumClass}. See class-level doc — this helper is for DB row
     * mapping; use a different code path to validate caller-supplied ordinals.
     *
     * @throws IllegalStateException if {@code ordinal} is negative or greater than or equal to the number of enum
     *                               constants
     */
    public static <E extends Enum<E>> E fromOrdinal(int ordinal, Class<E> enumClass) {
        E[] all = enumClass.getEnumConstants();

        if (ordinal < 0 || ordinal >= all.length) {
            throw new IllegalStateException(
                "Unknown " + enumClass.getSimpleName() + " ordinal " + ordinal + "; valid range [0, " + all.length +
                    ")");
        }

        return all[ordinal];
    }
}
