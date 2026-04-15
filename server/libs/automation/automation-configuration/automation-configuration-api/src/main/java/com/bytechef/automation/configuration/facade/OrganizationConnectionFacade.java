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

package com.bytechef.automation.configuration.facade;

import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * Facade for organization-scoped connections — connections visible to every member across every workspace.
 *
 * <p>
 * <b>EE-only.</b> The impl guards entry points with {@code validateEeEdition()}; CE callers see
 * {@code UnsupportedOperationException}. Authorization is enforced at the GraphQL controller with
 * {@code @PreAuthorize(ROLE_ADMIN)} — non-admin users receive 403 before reaching the facade.
 *
 * <p>
 * <b>Terminal visibility.</b> {@code create} always persists with
 * {@link com.bytechef.platform.connection.domain.ConnectionVisibility#ORGANIZATION}. Admins cannot demote an
 * organization connection to a narrower visibility — they must delete and recreate. This keeps organization-level
 * credentials from silently being narrowed to workspace-only by a routine share-list edit.
 *
 * @author Ivica Cardic
 */
public interface OrganizationConnectionFacade {

    long create(ConnectionDTO connectionDTO);

    void delete(long connectionId);

    List<ConnectionDTO> getOrganizationConnections(Long environmentId);

    ConnectionDTO update(long connectionId, String name, List<Long> tagIds, int version);
}
