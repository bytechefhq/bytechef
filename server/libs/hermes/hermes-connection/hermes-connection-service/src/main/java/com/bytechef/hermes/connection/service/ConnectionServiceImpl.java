
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

package com.bytechef.hermes.connection.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.repository.ConnectionRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Service("connectionService")
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(Connection connection) {
        Assert.notNull(connection, "'connection' must not be null");
        Assert.hasText(connection.getComponentName(), "'componentName' must not be empty");
        Assert.hasText(connection.getName(), "'name' must not be empty");
        Assert.isNull(connection.getId(), "'id' must be null");

        return connectionRepository.save(connection);
    }

    @Override
    public void delete(long id) {
        connectionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return OptionalUtils.get(connectionRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections() {
        return com.bytechef.commons.util.CollectionUtils.toList(connectionRepository.findAll(Sort.by("name")));
    }

    @Override
    public List<Connection> getConnections(List<Long> ids) {
        return com.bytechef.commons.util.CollectionUtils.toList(connectionRepository.findAllById(ids));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(List<String> componentNames, List<Long> tagIds) {
        Iterable<Connection> connectionIterable;

        if (CollectionUtils.isEmpty(componentNames) && CollectionUtils.isEmpty(tagIds)) {
            connectionIterable = connectionRepository.findAll(Sort.by("name"));
        } else if (!CollectionUtils.isEmpty(componentNames) && CollectionUtils.isEmpty(tagIds)) {
            connectionIterable = connectionRepository.findAllByComponentNameInOrderByName(componentNames);
        } else if (CollectionUtils.isEmpty(componentNames)) {
            connectionIterable = connectionRepository.findAllByTagIdIn(tagIds);
        } else {
            connectionIterable = connectionRepository.findAllByComponentNamesAndTagIds(componentNames, tagIds);
        }

        return com.bytechef.commons.util.CollectionUtils.toList(connectionIterable);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        Connection connection = getConnection(id);

        connection.setTagIds(tagIds);

        return connectionRepository.save(connection);
    }

    @Override
    @SuppressFBWarnings("NP")
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        Assert.hasText(name, "'name' must not be empty");

        Connection connection = OptionalUtils.get(connectionRepository.findById(id));

        connection.setName(name);
        connection.setTagIds(tagIds);
        connection.setVersion(version);

        return connectionRepository.save(connection);
    }
}
