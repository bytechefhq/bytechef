
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
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
    public Optional<Connection> fetchConnection(long id) {
        return connectionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections() {
        return connectionRepository.findAll(Sort.by("name"));
    }

    @Override
    public List<Connection> getConnections(List<Long> ids) {
        return connectionRepository.findAllById(ids);
    }

    @Override
    public List<Connection> getConnections(String componentName, int version) {
        return connectionRepository.findAllByComponentNameAndConnectionVersionOrderByName(componentName, version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, Integer connectionVersion, Long tagId) {
        Iterable<Connection> connectionIterable;

        if (!StringUtils.hasText(componentName) && tagId == null) {
            connectionIterable = connectionRepository.findAll(Sort.by("name"));
        } else if (StringUtils.hasText(componentName) && tagId == null) {
            if (connectionVersion == null) {
                connectionIterable = connectionRepository.findAllByComponentNameOrderByName(componentName);
            } else {
                connectionIterable = connectionRepository.findAllByComponentNameAndConnectionVersionOrderByName(
                    componentName, connectionVersion);
            }
        } else if (!StringUtils.hasText(componentName)) {
            connectionIterable = connectionRepository.findAllByTagId(tagId);
        } else {
            if (connectionVersion == null) {
                connectionIterable = connectionRepository.findAllByComponentNameAndTagId(componentName, tagId);
            } else {
                connectionIterable = connectionRepository.findAllByCNCVTI(componentName, connectionVersion, tagId);
            }

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
    public Connection update(Connection connection) {
        Assert.notNull(connection.getId(), "'id' must not be null");
        Assert.hasText(connection.getName(), "'name' must not be empty");

        Connection curConnection = OptionalUtils.get(connectionRepository.findById(connection.getId()));

        curConnection.setName(connection.getName());
        curConnection.setTagIds(connection.getTagIds());
        curConnection.setVersion(connection.getVersion());

        return connectionRepository.save(curConnection);
    }
}
