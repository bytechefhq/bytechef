/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EnumOrdinals {

    private EnumOrdinals() {
    }

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
