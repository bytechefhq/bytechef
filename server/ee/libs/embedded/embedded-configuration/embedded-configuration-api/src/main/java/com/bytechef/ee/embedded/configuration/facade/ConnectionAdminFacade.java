/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectionAdminFacade {

    long createConnection(ConnectionDTO connectionDTO, boolean shared);

    void deleteConnection(long id);

    ConnectionDTO getConnection(long id);

    List<ConnectionDTO> getConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Long environmentId,
        @Nullable Long tagId);

    Set<Long> getSharedConnectionIds();

    void updateConnection(long id, String name, List<Tag> tags, @Nullable Boolean shared, int version);
}
