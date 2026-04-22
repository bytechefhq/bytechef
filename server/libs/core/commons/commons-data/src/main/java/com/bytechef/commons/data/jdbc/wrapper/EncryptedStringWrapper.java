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

package com.bytechef.commons.data.jdbc.wrapper;

import java.util.Objects;

/**
 * Wrapper for a single String value that should be encrypted at rest in the database. Spring Data JDBC converters
 * handle transparent encryption/decryption when this type is used as a field type.
 *
 * @author Ivica Cardic
 */
public final class EncryptedStringWrapper {

    private String value;

    public EncryptedStringWrapper() {
    }

    public EncryptedStringWrapper(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EncryptedStringWrapper that = (EncryptedStringWrapper) o;

        return Objects.equals(value, that.value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "EncryptedStringWrapper{value=***}";
    }
}
