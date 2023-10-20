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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskAuthDescriptorHandlerRepositoryTest {

    private InMemoryExtTaskAuthDescriptorHandlerRepository inMemoryExtTaskAuthDescriptorHandlerRepository = new InMemoryExtTaskAuthDescriptorHandlerRepository();

    @Test
    public void testCreate() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskAuthDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testDelete() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name", "type");

        inMemoryExtTaskAuthDescriptorHandlerRepository.delete("name");

        Assertions.assertFalse(inMemoryExtTaskAuthDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testExistByNameAndType() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryExtTaskAuthDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testFindAll() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(2, inMemoryExtTaskAuthDescriptorHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAllNamesByType() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name2", "type2");

        List<String> names = inMemoryExtTaskAuthDescriptorHandlerRepository.findAllTaskNamesByType("type2");

        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("name2", names.get(0));
    }

    @Test
    public void testFindTypeByName() {
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name1", "type1");
        inMemoryExtTaskAuthDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals("type2", inMemoryExtTaskAuthDescriptorHandlerRepository.findTypeByTaskName("name2"));
    }
}
