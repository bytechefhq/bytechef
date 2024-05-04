/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.definition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a component.
 *
 * @author Ivica Cardic
 */
public interface ComponentDefinition {

    /**
     *
     * @return
     */
    Optional<List<? extends ActionDefinition>> getActions();

    /**
     * TODO
     *
     * @return
     */
    Optional<String> getCategory();

    /**
     *
     * @return
     */
    Optional<ConnectionDefinition> getConnection();

    /**
     *
     * @return
     */
    Optional<Boolean> getCustomAction();

    /**
     *
     * @return
     */
    Optional<Help> getCustomActionHelp();

    /**
     *
     * @return
     */
    Optional<DataStreamItemReader> getDataStreamItemReader();

    /**
     *
     * @return
     */
    Optional<DataStreamItemWriter> getDataStreamItemWriter();

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
     *
     * @return
     */
    Optional<Map<String, Object>> getMetadata();

    /**
     *
     * @return
     */
    String getName();

    /**
     * TODO
     *
     * @return
     */
    Optional<Resources> getResources();

    /**
     * TODO
     *
     * @return
     */
    Optional<List<String>> getTags();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    Optional<List<? extends TriggerDefinition>> getTriggers();

    /**
     * TODO
     *
     * @return
     */
    int getVersion();
}
