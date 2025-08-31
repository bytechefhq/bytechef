/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.fasade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionFacadeClient implements ConnectionFacade {

    @Override
    public long create(ConnectionDTO connectionDTO, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectionDTO getConnection(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(List<Long> connectionIds, ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        ModeType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Tag> getConnectionTags(ModeType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(long id, List<Tag> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(long id, String name, List<Tag> tags, int version) {
        throw new UnsupportedOperationException();
    }
}
