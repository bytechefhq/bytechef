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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final AuthenticationRepository authenticationRepository;

    public AuthenticationServiceImpl(AuthenticationRepository authenticationRepository) {
        this.authenticationRepository = authenticationRepository;
    }

    @Override
    public Authentication create(String name, String type, Map<String, Object> properties) {
        SimpleAuthentication simpleAuthentication = new SimpleAuthentication();

        simpleAuthentication.setCreateTime(new Date());
        simpleAuthentication.setId(UUIDGenerator.generate());
        simpleAuthentication.setName(name);
        simpleAuthentication.setProperties(properties);
        simpleAuthentication.setType(type);
        simpleAuthentication.setUpdateTime(new Date());

        authenticationRepository.create(simpleAuthentication);

        return simpleAuthentication;
    }

    @Override
    public void delete(String id) {
        authenticationRepository.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Authentication fetchAuthentication(String id) {
        return authenticationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Authentication> getAuthentications() {
        return authenticationRepository.findAll();
    }

    @Override
    public Authentication update(String id, String name) {
        SimpleAuthentication simpleAuthentication = new SimpleAuthentication(authenticationRepository.findById(id));

        simpleAuthentication.setName(name);
        simpleAuthentication.setUpdateTime(new Date());

        return authenticationRepository.update(simpleAuthentication);
    }
}
