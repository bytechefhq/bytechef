/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.http.client;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class HttpClientComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/http-client_v1.json", new HttpClientComponentHandler().getDefinition());
    }

    @Disabled
    @Test
    public void testPerformDelete() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformGet() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformHead() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformPatch() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformPost() {
        // TODO
    }

    @Disabled
    @Test
    public void testPerformPut() {
        // TODO
    }
}
