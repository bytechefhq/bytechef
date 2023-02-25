
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableConnectionDefinition.class)
public sealed interface ConnectionDefinition permits ComponentDSL.ModifiableConnectionDefinition {

    Boolean isAuthorizationRequired();

    List<? extends Authorization> getAuthorizations();

    Function<Connection, String> getBaseUriFunction();

    String getComponentName();

    Display getDisplay();

    List<? extends Property<?>> getProperties();

    Resources getResources();

    Optional<Consumer<Connection>> getTestConsumer();

    int getVersion();
}
