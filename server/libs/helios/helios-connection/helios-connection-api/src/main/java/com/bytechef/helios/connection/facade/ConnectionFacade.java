
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

package com.bytechef.helios.connection.facade;

import com.bytechef.helios.connection.dto.ConnectionDTO;
import com.bytechef.tag.domain.Tag;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface ConnectionFacade {

    ConnectionDTO create(ConnectionDTO connectionDTO);

    void delete(Long id);

    ConnectionDTO getConnection(Long id);

    List<ConnectionDTO> getConnections(String componentName, Integer componentVersion);

    List<ConnectionDTO> search(List<String> componentNames, List<Long> tagIds);

    List<Tag> getConnectionTags();

    ConnectionDTO update(Long id, List<Tag> tags);

    ConnectionDTO update(ConnectionDTO connectionDTO);
}
