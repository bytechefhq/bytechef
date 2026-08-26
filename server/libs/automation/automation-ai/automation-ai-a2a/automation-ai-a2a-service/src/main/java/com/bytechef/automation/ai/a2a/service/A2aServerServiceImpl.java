/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.a2a.service;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link A2aServerService} interface.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aServerServiceImpl implements A2aServerService {

    private final A2aServerRepository a2aServerRepository;

    public A2aServerServiceImpl(A2aServerRepository a2aServerRepository) {
        this.a2aServerRepository = a2aServerRepository;
    }

    @Override
    public A2aServer create(A2aServer a2aServer) {
        if (a2aServer.getUuid() == null) {
            a2aServer.setUuid(UUID.randomUUID());
        }

        return a2aServerRepository.save(a2aServer);
    }

    @Override
    public A2aServer create(String name, String description, PlatformType type, Environment environment) {
        A2aServer a2aServer = new A2aServer(name, description, type, environment);

        return create(a2aServer);
    }

    @Override
    public void delete(long a2aServerId) {
        a2aServerRepository.deleteById(a2aServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<A2aServer> fetchA2aServer(long a2aServerId) {
        return a2aServerRepository.findById(a2aServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<A2aServer> fetchA2aServer(UUID uuid, Environment environment) {
        return a2aServerRepository.findByUuidAndEnvironment(uuid, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public A2aServer getA2aServer(long a2aServerId) {
        return a2aServerRepository.findById(a2aServerId)
            .orElseThrow(() -> new IllegalArgumentException("A2A server with id " + a2aServerId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public A2aServer getA2aServer(String secretKey) {
        return a2aServerRepository.findBySecretKey(secretKey)
            .orElseThrow(
                () -> new IllegalArgumentException("A2A server with secret key " + secretKey + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<A2aServer> getA2aServers(PlatformType type) {
        return a2aServerRepository.findAllByType(type.ordinal());
    }

    @Override
    public A2aServer update(A2aServer a2aServer) {
        A2aServer currentA2aServer = a2aServerRepository.findById(a2aServer.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("A2A server with id " + a2aServer.getId() + " not found"));

        currentA2aServer.setName(a2aServer.getName());
        currentA2aServer.setDescription(a2aServer.getDescription());
        currentA2aServer.setEnabled(a2aServer.isEnabled());
        currentA2aServer.setAuthenticationRequired(a2aServer.isAuthenticationRequired());
        currentA2aServer.setEnvironment(a2aServer.getEnvironment());
        currentA2aServer.setVersion(a2aServer.getVersion());

        return a2aServerRepository.save(currentA2aServer);
    }
}
