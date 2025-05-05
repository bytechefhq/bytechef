/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.helper.util;

/**
 * @author Nikolina Spehar
 */
public enum CryptographicAlgorithm {

    MD5("MD5", "HmacMD5"),
    SHA_1("SHA-1", "HmacSHA1"),
    SHA_256("SHA-256", "HmacSHA256");

    private final String name;
    private final String label;

    CryptographicAlgorithm(String name, String label) {
        this.name = name;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
}
