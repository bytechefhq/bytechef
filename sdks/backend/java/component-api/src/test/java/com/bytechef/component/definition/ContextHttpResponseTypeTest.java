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

package com.bytechef.component.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http.ResponseType;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ContextHttpResponseTypeTest {

    @Test
    void testToStringOfConstantNamesTypeAndContentType() {
        assertEquals("JSON (application/json)", ResponseType.JSON.toString());
        assertEquals("TEXT (text/plain)", ResponseType.TEXT.toString());
        assertEquals("XML (application/xml)", ResponseType.XML.toString());
        assertEquals("BINARY (application/octet-stream)", ResponseType.BINARY.toString());
    }

    @Test
    void testToStringOfCustomBinaryKeepsContentType() {
        ResponseType binary = ResponseType.binary("image/png");

        assertEquals("BINARY (image/png)", binary.toString());
    }
}
