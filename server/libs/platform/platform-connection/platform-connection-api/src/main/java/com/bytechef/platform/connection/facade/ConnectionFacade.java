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

package com.bytechef.platform.connection.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ConnectionFacade {

    long create(ConnectionDTO connectionDTO, PlatformType type);

    void delete(Long id);

    ConnectionDTO getConnection(Long id);

    List<ConnectionDTO> getConnections(List<Long> connectionIds, PlatformType type);

    List<ConnectionDTO> getConnections(
        String componentName, Integer connectionVersion, List<Long> connectionIds, Long tagId, Long environmentId,
        PlatformType type);

    List<Tag> getConnectionTags(PlatformType type);

    void update(long id, List<Tag> tags);

    void update(long id, String name, List<Tag> tags, int version);
}
