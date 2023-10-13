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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Property.ValueProperty;
import com.bytechef.hermes.definition.Resources;
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
    Optional<OutputProperty<?>> getOutputSchema();

    /**
     *
     * @return
     */
    Optional<List<? extends InputProperty>> getProperties();

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
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    int getVersion();

    /**
     *
     * @return
     */
    Optional<List<? extends ValueProperty<?>>> getTaskProperties();
}
