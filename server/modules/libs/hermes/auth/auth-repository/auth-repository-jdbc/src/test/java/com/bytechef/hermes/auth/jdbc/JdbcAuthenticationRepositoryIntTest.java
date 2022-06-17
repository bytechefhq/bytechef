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

package com.bytechef.hermes.auth.jdbc;

import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.domain.SimpleAuthentication;
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
public class JdbcAuthenticationRepositoryIntTest {

    @Autowired
    private JdbcAuthenticationRepository jdbcAuthenticationRepository;

    @BeforeEach
    public void beforeEach() {
        List<Authentication> authentications = jdbcAuthenticationRepository.findAll();

        for (Authentication authentication : authentications) {
            jdbcAuthenticationRepository.delete(authentication.getId());
        }
    }

    @Test
    public void testCreate() {
        SimpleAuthentication authentication = getSimpleAuthentication();

        jdbcAuthenticationRepository.create(authentication);

        Assertions.assertEquals(authentication, jdbcAuthenticationRepository.findById(authentication.getId()));
    }

    @Test
    public void testDelete() {
        SimpleAuthentication authentication = getSimpleAuthentication();

        jdbcAuthenticationRepository.create(authentication);

        Assertions.assertEquals(1, jdbcAuthenticationRepository.findAll().size());

        jdbcAuthenticationRepository.delete(authentication.getId());

        Assertions.assertEquals(0, jdbcAuthenticationRepository.findAll().size());
    }

    @Test
    public void testFindAll() {
        SimpleAuthentication authentication = getSimpleAuthentication();

        jdbcAuthenticationRepository.create(authentication);

        Assertions.assertEquals(1, jdbcAuthenticationRepository.findAll().size());
    }

    @Test
    public void testFindById() {
        SimpleAuthentication authentication = getSimpleAuthentication();

        jdbcAuthenticationRepository.create(authentication);

        Assertions.assertEquals(authentication, jdbcAuthenticationRepository.findById(authentication.getId()));
    }

    @Test
    public void testMerge() {
        SimpleAuthentication authentication = getSimpleAuthentication();

        jdbcAuthenticationRepository.create(authentication);

        authentication.setName("name2");
        authentication.setProperties(Map.of("key2", "value2"));
        authentication.setUpdateTime(new Date());
        authentication.setType("type2");

        jdbcAuthenticationRepository.update(authentication);

        Authentication updatedAuthentication = jdbcAuthenticationRepository.findById(authentication.getId());

        Assertions.assertEquals(authentication.getName(), updatedAuthentication.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), updatedAuthentication.getProperties());
        Assertions.assertEquals(authentication.getUpdateTime(), updatedAuthentication.getUpdateTime());
        Assertions.assertEquals("type", updatedAuthentication.getType());
    }

    private static SimpleAuthentication getSimpleAuthentication() {
        SimpleAuthentication authentication = new SimpleAuthentication();

        authentication.setName("name");
        authentication.setId(UUIDGenerator.generate());
        authentication.setCreateTime(new Date());
        authentication.setProperties(Map.of("key1", "value1"));
        authentication.setUpdateTime(new Date());
        authentication.setType("type");

        return authentication;
    }
}
