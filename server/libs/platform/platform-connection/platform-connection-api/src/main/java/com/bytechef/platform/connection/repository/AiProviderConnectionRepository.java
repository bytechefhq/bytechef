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

package com.bytechef.platform.connection.repository;

import com.bytechef.platform.connection.domain.Connection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Projects enabled AI providers (from the {@code property} table and {@code application.yml}) into read-only
 * {@link Connection} instances. Not a Spring Data repository — every returned connection is built in memory with a
 * negative {@link com.bytechef.platform.connection.domain.AiProviderConnectionId} and {@code managed = true}.
 *
 * @author Ivica Cardic
 */
public interface AiProviderConnectionRepository {

    Optional<Connection> findById(long id);

    List<Connection> find(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId);

    List<Connection> findAllByIdIn(List<Long> ids);
}
