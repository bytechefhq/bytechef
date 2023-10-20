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

package com.bytechef.hermes.descriptor.repository.memory;

import com.bytechef.hermes.descriptor.repository.ExtTaskDescriptorHandlerRepository;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtTaskDescriptorHandlerRepositoryTest {

    private InMemoryExtTaskDescriptorHandlerRepository inMemoryExtTaskDescriptorHandlerRepository =
            new InMemoryExtTaskDescriptorHandlerRepository();

    @Test
    public void testCreate() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", 1.0f, "type");

        Assertions.assertTrue(
                inMemoryExtTaskDescriptorHandlerRepository.existByNameAndVersionAndType("name", 1.0f, "type"));
    }

    @Test
    public void testDelete() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", 1.0f, "type");

        inMemoryExtTaskDescriptorHandlerRepository.delete("name", 1.0f);

        Assertions.assertFalse(
                inMemoryExtTaskDescriptorHandlerRepository.existByNameAndVersionAndType("name", 1.0f, "type"));
    }

    @Test
    public void testExistByNameAndType() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name", 1.0f, "type");

        Assertions.assertTrue(
                inMemoryExtTaskDescriptorHandlerRepository.existByNameAndVersionAndType("name", 1.0f, "type"));
    }

    @Test
    public void testFindAll() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", 1.0f, "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", 1.0f, "type2");

        Assertions.assertEquals(
                2, inMemoryExtTaskDescriptorHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAllNamesByType() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", 1.0f, "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", 1.1f, "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", 1.0f, "type2");

        List<ExtTaskDescriptorHandlerRepository.NameVersions> nameVersions =
                inMemoryExtTaskDescriptorHandlerRepository.findAllNamesByType("type2");

        Assertions.assertEquals(1, nameVersions.size());
        Assertions.assertEquals("name2", nameVersions.get(0).name());
    }

    @Test
    public void testFindTypeByNameAndVersion() {
        inMemoryExtTaskDescriptorHandlerRepository.create("name1", 1.0f, "type1");
        inMemoryExtTaskDescriptorHandlerRepository.create("name2", 1.0f, "type2");

        Assertions.assertEquals(
                "type2", inMemoryExtTaskDescriptorHandlerRepository.findTypeByNameAndVersion("name2", 1.0f));
    }
}
