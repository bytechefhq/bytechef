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

package com.bytechef.platform.workflow.task.dispatcher.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class Option {

    private String description;
    private String displayCondition;
    private String label;
    private Object value;

    private Option() {
    }

    public Option(com.bytechef.platform.workflow.task.dispatcher.definition.Option<?> option) {
        this.description = OptionalUtils.orElse(option.getDescription(), null);
        this.displayCondition = option.getDisplayCondition();
        this.label = option.getLabel();
        this.value = option.getValue();
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public String getDisplayCondition() {
        return displayCondition;
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Option option)) {
            return false;
        }

        return Objects.equals(description, option.description)
            && Objects.equals(displayCondition, option.displayCondition) && Objects.equals(label, option.label)
            && Objects.equals(value, option.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, displayCondition, label, value);
    }

    @Override
    public String toString() {
        return "OptionDTO{" +
            "description='" + description + '\'' +
            ", displayCondition='" + displayCondition + '\'' +
            ", label='" + label + '\'' +
            ", value=" + value +
            '}';
    }
}
