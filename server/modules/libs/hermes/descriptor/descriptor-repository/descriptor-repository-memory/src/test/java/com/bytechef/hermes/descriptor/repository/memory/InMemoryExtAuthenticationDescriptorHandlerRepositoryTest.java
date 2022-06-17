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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class InMemoryExtAuthenticationDescriptorHandlerRepositoryTest {

    private InMemoryExtAuthenticationDescriptorHandlerRepository inMemoryAuthenticationDescriptorHandlerRepository =
            new InMemoryExtAuthenticationDescriptorHandlerRepository();

    @Test
    public void testCreate() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryAuthenticationDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testDelete() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name", "type");

        inMemoryAuthenticationDescriptorHandlerRepository.delete("name");

        Assertions.assertFalse(
                inMemoryAuthenticationDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testExistByNameAndType() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name", "type");

        Assertions.assertTrue(inMemoryAuthenticationDescriptorHandlerRepository.existByTaskNameAndType("name", "type"));
    }

    @Test
    public void testFindAll() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name1", "type1");
        inMemoryAuthenticationDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals(
                2, inMemoryAuthenticationDescriptorHandlerRepository.findAll().size());
    }

    @Test
    public void testFindAllNamesByType() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name1", "type1");
        inMemoryAuthenticationDescriptorHandlerRepository.create("name2", "type2");

        List<String> names = inMemoryAuthenticationDescriptorHandlerRepository.findAllByType("type2");

        Assertions.assertEquals(1, names.size());
        Assertions.assertEquals("name2", names.get(0));
    }

    @Test
    public void testFindTypeByName() {
        inMemoryAuthenticationDescriptorHandlerRepository.create("name1", "type1");
        inMemoryAuthenticationDescriptorHandlerRepository.create("name2", "type2");

        Assertions.assertEquals("type2", inMemoryAuthenticationDescriptorHandlerRepository.findTypeByTaskName("name2"));
    }
}
