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

package com.bytechef.component.data.mapper;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class DataMapperComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/data-mapper_v1.json", new DataMapperComponentHandler().getDefinition());
    }

    @Test
    @Disabled
    public void testMapKeysAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapListToObjectAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapMultipleValuesBetweenObjectsAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapObjectsAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapObjectsToListAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapOneValueAction() {
        // TODO
    }

    @Test
    @Disabled
    public void testMapValuesAction() {
        // TODO
    }
}
