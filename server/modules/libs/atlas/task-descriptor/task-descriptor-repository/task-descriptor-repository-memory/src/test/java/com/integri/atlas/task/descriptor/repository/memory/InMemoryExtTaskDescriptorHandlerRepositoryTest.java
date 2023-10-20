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

package com.integri.atlas.task.descriptor.repository.memory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskDescriptorHandlerRepositoryTest {

    private InMemoryExtTaskDescriptorHandlerRepository inMemoryExtTaskDescriptorHandlerRepository = new InMemoryExtTaskDescriptorHandlerRepository();

    @Test
    public void testCreate() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDescriptorHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testDelete() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", "type");

        inMemoryExtTaskDescriptorHandlerRepository.delete("name");

        Assertions.assertFalse(inMemoryExtTaskDescriptorHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testExistByNameAndType() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDescriptorHandlerRepository.existByNameAndType("name", "type"));
    }

    @Test
    public void testFindAll() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(2, inMemoryExtTaskDescriptorHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAllNamesByType() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(1, inMemoryExtTaskDescriptorHandlerRepository.findAllNamesByType("type2").size());
    }

    @Test
    public void testFindTypeByName() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals("type2", inMemoryExtTaskDescriptorHandlerRepository.findTypeByName("name2"));
    }

    @Test
    public void testUpdate() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskDescriptorHandlerRepository.existByNameAndType("name", "type"));

        inMemoryExtTaskDescriptorHandlerRepository.update("name", "type2");

        Assertions.assertTrue(inMemoryExtTaskDescriptorHandlerRepository.existByNameAndType("name", "type2"));
    }
}
