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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class HttpHeadersImplTest {

    @Test
    void testFirstValueIgnoresHeaderNameCase() {
        HttpHeadersImpl httpHeaders = new HttpHeadersImpl(Map.of("x-csrf-token", List.of("secret")));

        assertThat(httpHeaders.firstValue("X-Csrf-Token")).hasValue("secret");
        assertThat(httpHeaders.firstValue("X-CSRF-TOKEN")).hasValue("secret");
    }

    @Test
    void testFirstValueReturnsEmptyForMissingHeader() {
        HttpHeadersImpl httpHeaders = new HttpHeadersImpl(Map.of("Content-Type", List.of("application/json")));

        assertThat(httpHeaders.firstValue("X-Csrf-Token")).isEmpty();
    }

    @Test
    void testAllValuesIgnoresHeaderNameCase() {
        HttpHeadersImpl httpHeaders = new HttpHeadersImpl(Map.of("accept", List.of("text/plain", "text/html")));

        assertThat(httpHeaders.allValues("Accept")).containsExactly("text/plain", "text/html");
    }

    @Test
    void testAllValuesReturnsOnlyValuesOfNamedHeader() {
        HttpHeadersImpl httpHeaders = new HttpHeadersImpl(
            Map.of("Accept", List.of("text/plain"), "Content-Type", List.of("application/json")));

        assertThat(httpHeaders.allValues("Accept")).containsExactly("text/plain");
        assertThat(httpHeaders.allValues("X-Missing")).isEmpty();
    }

    @Test
    void testToMapKeepsHeaderNamesAsSent() {
        HttpHeadersImpl httpHeaders = new HttpHeadersImpl(Map.of("X-Csrf-Token", List.of("secret")));

        assertThat(httpHeaders.toMap()).containsOnlyKeys("X-Csrf-Token");
    }
}
