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
import com.bytechef.component.definition.ActionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinitionBasic {

    protected boolean batch;
    protected String componentName;
    protected int componentVersion;
    protected String description;
    protected Help help;
    protected String name;
    protected String title;

    protected ActionDefinitionBasic() {
    }

    public ActionDefinitionBasic(
        ActionDefinition actionDefinition, String componentName, int componentVersion) {

        this.batch = OptionalUtils.orElse(actionDefinition.getBatch(), false);
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = Validate.notNull(getDescription(actionDefinition), "description");
        this.help = OptionalUtils.mapOrElse(actionDefinition.getHelp(), Help::new, null);
        this.name = Validate.notNull(actionDefinition.getName(), "name");
        this.title = Validate.notNull(getTitle(actionDefinition), "title");
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

    public boolean isBatch() {
        return batch;
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

    private static String getDescription(ActionDefinition actionDefinition) {
        return OptionalUtils.orElse(
            actionDefinition.getDescription(),
            OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName()));
    }

    private static String getTitle(ActionDefinition actionDefinition) {
        return OptionalUtils.orElse(actionDefinition.getTitle(), actionDefinition.getName());
    }
}
