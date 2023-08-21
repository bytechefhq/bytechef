
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TriggerDefinitionBasicDTO {

    protected final boolean batch;
    protected final String description;
    protected final HelpDTO help;
    protected final String name;
    protected final String title;
    protected final TriggerType type;

    public TriggerDefinitionBasicDTO(TriggerDefinition triggerDefinition) {
        this.batch = OptionalUtils.orElse(triggerDefinition.getBatch(), false);
        this.description = Objects.requireNonNull(getDescription(triggerDefinition));
        this.help = OptionalUtils.mapOrElse(triggerDefinition.getHelp(), HelpDTO::new, null);
        this.name = Objects.requireNonNull(triggerDefinition.getName());
        this.title = Objects.requireNonNull(getTitle(triggerDefinition));
        this.type = Objects.requireNonNull(triggerDefinition.getType());
    }

    public boolean isBatch() {
        return batch;
    }

    public String getDescription() {
        return description;
    }

    public HelpDTO getHelp() {
        return help;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public TriggerType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TriggerDefinitionBasicDTO that))
            return false;
        return batch == that.batch && Objects.equals(description, that.description) && Objects.equals(help, that.help)
            && Objects.equals(name, that.name) && Objects.equals(title, that.title) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, description, help, name, title, type);
    }

    @Override
    public String toString() {
        return "TriggerDefinitionBasicDTO{" +
            "batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", type=" + type +
            '}';
    }

    private static String getDescription(TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(
            triggerDefinition.getDescription(),
            OptionalUtils.orElse(triggerDefinition.getComponentTitle(), triggerDefinition.getComponentName()) +
                ": " +
                getTitle(triggerDefinition));
    }

    private static String getTitle(TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(triggerDefinition.getTitle(), triggerDefinition.getName());
    }
}
