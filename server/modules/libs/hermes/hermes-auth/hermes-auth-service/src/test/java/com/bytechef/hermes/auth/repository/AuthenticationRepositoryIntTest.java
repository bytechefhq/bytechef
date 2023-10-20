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

package com.bytechef.hermes.auth.repository;

import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.hermes.auth.domain.Authentication;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class AuthenticationRepositoryIntTest {

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @BeforeEach
    public void beforeEach() {
        List<Authentication> authentications = authenticationRepository.findAll();

        for (Authentication authentication : authentications) {
            authenticationRepository.delete(authentication.getId());
        }
    }

    @Test
    public void testCreate() {
        Authentication authentication = getAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(authentication, authenticationRepository.findById(authentication.getId()));
    }

    @Test
    public void testDelete() {
        Authentication authentication = getAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(1, authenticationRepository.findAll().size());

        authenticationRepository.delete(authentication.getId());

        Assertions.assertEquals(0, authenticationRepository.findAll().size());
    }

    @Test
    public void testFindAll() {
        Authentication authentication = getAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(1, authenticationRepository.findAll().size());
    }

    @Test
    public void testFindById() {
        Authentication authentication = getAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(authentication, authenticationRepository.findById(authentication.getId()));
    }

    @Test
    public void testMerge() {
        Authentication authentication = getAuthentication();

        authenticationRepository.create(authentication);

        authentication.setName("name2");
        authentication.setProperties(Map.of("key2", "value2"));
        authentication.setUpdateTime(new Date());
        authentication.setType("type2");

        authenticationRepository.update(authentication);

        Authentication updatedAuthentication = authenticationRepository.findById(authentication.getId());

        Assertions.assertEquals(authentication.getName(), updatedAuthentication.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), updatedAuthentication.getProperties());
        Assertions.assertEquals(authentication.getUpdateTime(), updatedAuthentication.getUpdateTime());
        Assertions.assertEquals("type", updatedAuthentication.getType());
    }

    private static Authentication getAuthentication() {
        Authentication authentication = new Authentication();

        authentication.setId(UUIDGenerator.generate());
        authentication.setName("name");
        authentication.setType("type");
        authentication.setProperties(Map.of("key1", "value1"));
        authentication.setUpdateTime(new Date());
        authentication.setCreateTime(new Date());

        return authentication;
    }
}
