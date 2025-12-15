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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.Resources;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface JdbcComponentDefinition {

    /**
     * URL template used to construct JDBC connection URL. Supported placeholders: {host}, {port}, {database}
     *
     * @return JDBC URL template for the specific database
     */
    String getUrlTemplate();

    /**
     *
     * @return
     */
    String getJdbcDriverClassName();

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    /**
     *
     * @return
     */
    Optional<String> getIcon();

    /**
     * TODO
     *
     * @return
     */
    Optional<Resources> getResources();

    /**
     *
     * @return
     */
    String getName();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     * TODO
     *
     * @return
     */
    int getVersion();
}
