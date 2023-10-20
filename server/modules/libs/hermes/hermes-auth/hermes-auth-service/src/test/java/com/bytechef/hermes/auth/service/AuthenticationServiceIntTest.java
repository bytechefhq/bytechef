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

package com.bytechef.hermes.auth.service;

import com.bytechef.atlas.uuid.UUIDGenerator;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.domain.SimpleAuthentication;
import com.bytechef.hermes.auth.repository.AuthenticationRepository;
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
public class AuthenticationServiceIntTest {

    @Autowired
    private AuthenticationService authenticationService;

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
        Authentication authentication = authenticationService.create("name", "type", Map.of("key1", "value1"));

        Assertions.assertEquals("name", authentication.getName());
        Assertions.assertEquals(Map.of("key1", "value1"), authentication.getProperties());
        Assertions.assertEquals("type", authentication.getType());
    }

    @Test
    public void testDelete() {
        Authentication authentication = getSimpleAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(1, authenticationRepository.findAll().size());

        authenticationService.delete(authentication.getId());

        Assertions.assertEquals(0, authenticationRepository.findAll().size());
    }

    @Test
    public void testGetAuthentication() {
        Authentication authentication = getSimpleAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(authentication, authenticationService.fetchAuthentication(authentication.getId()));
    }

    @Test
    public void getGetAuthentications() {
        Authentication authentication = getSimpleAuthentication();

        authenticationRepository.create(authentication);

        Assertions.assertEquals(1, authenticationService.getAuthentications().size());
    }

    @Test
    public void testUpdate() {
        Authentication authentication = getSimpleAuthentication();

        authenticationRepository.create(authentication);

        Authentication updatedAuthentication = authenticationService.update(authentication.getId(), "name2");

        Assertions.assertEquals("name2", updatedAuthentication.getName());
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
