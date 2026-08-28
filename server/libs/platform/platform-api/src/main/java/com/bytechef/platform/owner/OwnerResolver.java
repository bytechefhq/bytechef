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

package com.bytechef.platform.owner;

import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;

/**
 * Resolves the {@link Owner} a running workflow or an inbound request belongs to.
 *
 * <p>
 * Implemented in Enterprise embedded, the only edition where a principal below the tenant exists. Community ships no
 * implementation, and an empty result there correctly means "no owner, see everything".
 *
 * @author Ivica Cardic
 */
public interface OwnerResolver {

    /**
     * @param jobPrincipalId a project-deployment id when {@code platformType} is {@code AUTOMATION}, an
     *                       integration-instance id when it is {@code EMBEDDED}
     * @param platformType   the platform the job runs under
     * @return the owner behind that job principal, or empty when the principal belongs to no connected user
     */
    Optional<Owner> resolveJobPrincipal(long jobPrincipalId, PlatformType platformType);

    /**
     * Resolves the owner from the current security context rather than from a job. Editor test runs have no persisted
     * job and therefore no job principal, so this is the only way to scope them.
     *
     * @return the owner, or empty when the caller is not a connected user
     */
    Optional<Owner> resolveCurrentPrincipal();
}
