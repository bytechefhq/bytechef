/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.component.data.storage;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class DataStorageComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/data-storage_v1.json", new DataStorageComponentHandler().getDefinition());
    }

    @Test
    @Disabled
    public void testSetValueInListAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testSetValueAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testGetValueAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testGetAllKeysAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testDeleteValueFromListAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testDeleteValueAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testAwaitGetValueAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testAtomicIncrementAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testAppendValueToListAction() {
        // TODO
    }
}
