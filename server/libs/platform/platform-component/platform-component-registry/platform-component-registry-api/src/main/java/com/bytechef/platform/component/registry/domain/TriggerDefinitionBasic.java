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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.registry.domain.Help;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TriggerDefinitionBasic {

    protected boolean batch;
    protected String description;
    protected String componentName;
    protected int componentVersion;
    protected Help help;
    protected String name;
    protected String title;
    protected TriggerType type;

    protected TriggerDefinitionBasic() {
    }

    public TriggerDefinitionBasic(TriggerDefinition triggerDefinition) {
        this.batch = OptionalUtils.orElse(triggerDefinition.getBatch(), false);
        this.componentName = triggerDefinition.getComponentName();
        this.componentVersion = triggerDefinition.getComponentVersion();
        this.description = Validate.notNull(getDescription(triggerDefinition), "description");
        this.help = OptionalUtils.mapOrElse(triggerDefinition.getHelp(), Help::new, null);
        this.name = Validate.notNull(triggerDefinition.getName(), "name");
        this.title = Validate.notNull(getTitle(triggerDefinition), "title");
        this.type = Validate.notNull(triggerDefinition.getType(), "type");
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getDescription() {
        return description;
    }

    public Help getHelp() {
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
        if (!(o instanceof TriggerDefinitionBasic that))
            return false;
        return batch == that.batch && Objects.equals(description, that.description) && Objects.equals(help, that.help)
            && Objects.equals(name, that.name) && Objects.equals(title, that.title) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, description, help, name, title, type);
    }

    public boolean isBatch() {
        return batch;
    }

    @Override
    public String toString() {
        return "TriggerDefinitionBasic{" +
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
