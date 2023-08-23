
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

package com.bytechef.hermes.definition.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinitionBasic {

    protected final boolean batch;
    protected final String description;
    protected final Help help;
    protected final String name;
    protected final String title;

    public ActionDefinitionBasic(ActionDefinition actionDefinition) {
        this.batch = OptionalUtils.orElse(actionDefinition.getBatch(), false);
        this.description = Objects.requireNonNull(getDescription(actionDefinition));
        this.help = OptionalUtils.mapOrElse(actionDefinition.getHelp(), Help::new, null);
        this.name = Objects.requireNonNull(actionDefinition.getName());
        this.title = Objects.requireNonNull(getTitle(actionDefinition));
    }

    public boolean isBatch() {
        return batch;
    }

    public String getDescription() {
        return description;
    }

    public Optional<Help> getHelp() {
        return Optional.ofNullable(help);
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    private static String getDescription(ActionDefinition actionDefinition) {
        return OptionalUtils.orElse(
            actionDefinition.getDescription(),
            OptionalUtils.orElse(actionDefinition.getComponentTitle(), actionDefinition.getComponentName()) +
                ": " +
                OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName()));
    }

    private static String getTitle(ActionDefinition actionDefinition) {
        return OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ActionDefinitionBasic that = (ActionDefinitionBasic) o;
        return batch == that.batch && Objects.equals(description, that.description) && Objects.equals(help, that.help)
            && Objects.equals(name, that.name) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, description, help, name, title);
    }

    @Override
    public String toString() {
        return "ActionDefinitionBasic{" +
            "batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            '}';
    }
}
