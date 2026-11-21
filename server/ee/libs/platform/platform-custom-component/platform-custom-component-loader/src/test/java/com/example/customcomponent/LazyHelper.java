/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package customcomponentfixture;

/**
 * @version ee
 */
public final class LazyHelper {

    private LazyHelper() {
    }

    public static String greet(String name) {
        return "Hello " + name;
    }
}
