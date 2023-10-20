/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.definition.repository.memory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskDefinitionHandlerRepositoryTest {

    private InMemoryExtTaskDefinitionHandlerRepository inMemoryExtTaskDefinitionHandlerRepository = new InMemoryExtTaskDefinitionHandlerRepository();

    @Test
    public void testCreate() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDefinitionHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testDelete() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name", "type");

        inMemoryExtTaskDefinitionHandlerRepository.delete("name");

        Assertions.assertFalse(inMemoryExtTaskDefinitionHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testExistByNameAndType() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDefinitionHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testFindAll() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDefinitionHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(2, inMemoryExtTaskDefinitionHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAllNamesByType() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDefinitionHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(1, inMemoryExtTaskDefinitionHandlerRepository.findAllNamesByType("type2").size());
    }

    @Test
    public void testFindTypeByName() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDefinitionHandlerRepository.create("name2", "type2");

        Assertions.assertEquals("type2", inMemoryExtTaskDefinitionHandlerRepository.findTypeByName("name2"));
    }

    @Test
    public void testUpdate() {
        inMemoryExtTaskDefinitionHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDefinitionHandlerRepository.existByNameAndType("name", "type"));

        inMemoryExtTaskDefinitionHandlerRepository.update("name", "type2");

        Assertions.assertTrue(
            inMemoryExtTaskDefinitionHandlerRepository.existByNameAndType("name", "type2")
        );
    }
}
