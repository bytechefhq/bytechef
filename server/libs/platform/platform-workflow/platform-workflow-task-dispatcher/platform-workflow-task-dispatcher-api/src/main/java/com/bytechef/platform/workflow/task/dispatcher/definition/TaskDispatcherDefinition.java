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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;

/**
 * Used for specifying a task dispatcher description.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public interface TaskDispatcherDefinition {

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    /**
     *
     * @return
     */
    Optional<Help> getHelp();

    /**
     *
     * @return
     */
    Optional<String> getIcon();

    /**
     *
     * @return
     */
    String getName();

    /**
     *
     * @return
     */
    Optional<Output> getOutput();

    /**
     *
     * @return
     */
    Optional<OutputFunction> getOutputFunction();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

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
    Optional<List<? extends Property>> getTaskProperties();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getVariableProperties();

    /**
     *
     * @return
     */
    Optional<VariablePropertiesFunction> getVariablePropertiesFunction();

    /**
     *
     * @return
     */
    int getVersion();
}
